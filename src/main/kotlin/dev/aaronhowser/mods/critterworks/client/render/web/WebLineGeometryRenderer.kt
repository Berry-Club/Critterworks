package dev.aaronhowser.mods.critterworks.client.render.web

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import dev.aaronhowser.mods.aaron.misc.AaronDsls.withPose
import dev.aaronhowser.mods.critterworks.Critterworks
import dev.aaronhowser.mods.critterworks.config.ClientConfig
import dev.aaronhowser.mods.critterworks.handler.web.line.ClientWebLines
import dev.aaronhowser.mods.critterworks.handler.web.line.WebLine
import net.minecraft.client.renderer.LevelRenderer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.core.BlockPos
import net.minecraft.util.Mth
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import org.joml.Quaternionf

object WebLineGeometryRenderer {

	fun renderAll(bufferSource: MultiBufferSource.BufferSource, level: Level, poseStack: PoseStack, cameraPosition: Vec3) {
		for (line in ClientWebLines.getLines()) {
			render(
				bufferSource,
				level,
				poseStack,
				line.firstNode.position,
				line.secondNode.position,
				cameraPosition,
				getColor(line)
			)
		}
	}

	fun render(
		bufferSource: MultiBufferSource.BufferSource,
		level: Level,
		poseStack: PoseStack,
		start: Vec3,
		end: Vec3,
		cameraPosition: Vec3,
		color: Int
	) {
		val offset = end.subtract(start)
		val height = offset.length()
		if (height == 0.0) return

		val vertexConsumer = bufferSource.getBuffer(WEB_RENDER_TYPE)
		val direction = offset.scale(1.0 / height)
		val rotation = Quaternionf().rotationTo(
			0f,
			1f,
			0f,
			direction.x.toFloat(),
			direction.y.toFloat(),
			direction.z.toFloat()
		)

		poseStack.withPose {
			val relativeStart = start.subtract(cameraPosition)
			poseStack.translate(relativeStart.x, relativeStart.y, relativeStart.z)
			poseStack.mulPose(rotation)

			val pose = poseStack.last()
			var segmentStart = 0.0
			var startLight = LevelRenderer.getLightColor(level, BlockPos.containing(start))

			while (segmentStart < height) {
				val segmentEnd = minOf(segmentStart + LIGHT_SAMPLE_DISTANCE, height)
				val endPosition = start.add(direction.scale(segmentEnd))
				val endLight = LevelRenderer.getLightColor(level, BlockPos.containing(endPosition))

				addSegment(
					vertexConsumer,
					pose,
					segmentStart,
					segmentEnd,
					startLight,
					endLight,
					color
				)

				segmentStart = segmentEnd
				startLight = endLight
			}
		}

		bufferSource.endBatch(WEB_RENDER_TYPE)
	}

	private fun getColor(line: WebLine): Int {
		if (!ClientConfig.CONFIG.renderWebLineDebugColors.get()) return WEB_COLOR

		val hash = line.uuid.mostSignificantBits xor line.uuid.leastSignificantBits
		val hue = (hash and 0xFFFF).toFloat() / 0x10000
		val rgb = Mth.hsvToRgb(hue, 0.9f, 1f)

		return 0xFF000000.toInt() or rgb
	}

	private fun addSegment(
		vertexConsumer: VertexConsumer,
		pose: PoseStack.Pose,
		start: Double,
		end: Double,
		startLight: Int,
		endLight: Int,
		color: Int
	) {
		addSide(
			vertexConsumer, pose,
			-WEB_RADIUS, -WEB_RADIUS,
			WEB_RADIUS, -WEB_RADIUS,
			1f, 0f, 0f,
			start, end,
			0f, 0.25f,
			startLight, endLight,
			color
		)

		addSide(
			vertexConsumer, pose,
			WEB_RADIUS, -WEB_RADIUS,
			WEB_RADIUS, WEB_RADIUS,
			0f, 1f, 0f,
			start, end,
			0.25f, 0.5f,
			startLight, endLight,
			color
		)

		addSide(
			vertexConsumer, pose,
			WEB_RADIUS, WEB_RADIUS,
			-WEB_RADIUS, WEB_RADIUS,
			1f, 0f, 0f,
			start, end,
			0.5f, 0.75f,
			startLight, endLight,
			color
		)

		addSide(
			vertexConsumer, pose,
			-WEB_RADIUS, WEB_RADIUS,
			-WEB_RADIUS, -WEB_RADIUS,
			0f, 1f, 0f,
			start, end,
			0.75f, 1f,
			startLight, endLight,
			color
		)
	}

	private fun addSide(
		vertexConsumer: VertexConsumer,
		pose: PoseStack.Pose,
		firstX: Float, firstZ: Float,
		secondX: Float, secondZ: Float,
		normalX: Float, normalY: Float, normalZ: Float,
		start: Double, end: Double,
		minU: Float, maxU: Float,
		startLight: Int, endLight: Int,
		color: Int
	) {
		val minV = (start / TEXTURE_REPEAT_DISTANCE).toFloat()
		val maxV = (end / TEXTURE_REPEAT_DISTANCE).toFloat()

		addVertex(vertexConsumer, pose, firstX, start.toFloat(), firstZ, minU, minV, startLight, color, normalX, normalY, normalZ)
		addVertex(vertexConsumer, pose, firstX, end.toFloat(), firstZ, minU, maxV, endLight, color, normalX, normalY, normalZ)
		addVertex(vertexConsumer, pose, secondX, end.toFloat(), secondZ, maxU, maxV, endLight, color, normalX, normalY, normalZ)
		addVertex(vertexConsumer, pose, secondX, start.toFloat(), secondZ, maxU, minV, startLight, color, normalX, normalY, normalZ)
	}

	private fun addVertex(
		vertexConsumer: VertexConsumer,
		pose: PoseStack.Pose,
		x: Float, y: Float, z: Float,
		u: Float, v: Float,
		light: Int, color: Int,
		normalX: Float, normalY: Float, normalZ: Float
	) {
		vertexConsumer.addVertex(pose.pose(), x, y, z)
			.setColor(color)
			.setUv(u, v)
			.setOverlay(OverlayTexture.NO_OVERLAY)
			.setLight(light)
			.setNormal(normalX, normalY, normalZ)
	}

	private const val WEB_COLOR = 0xFFFFFFFF.toInt()
	private const val WEB_RADIUS = 1f / 64f
	private const val TEXTURE_REPEAT_DISTANCE = 4.0
	private const val LIGHT_SAMPLE_DISTANCE = 1.0

	private val WEB_TEXTURE = Critterworks.modResource("textures/misc/web_line.png")
	private val WEB_RENDER_TYPE = RenderType.entitySolid(WEB_TEXTURE)
}
