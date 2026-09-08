package dev.aaronhowser.mods.critterworks.client.render.entity.layer

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.math.Axis
import dev.aaronhowser.mods.aaron.misc.AaronDsls.withPose
import dev.aaronhowser.mods.critterworks.entity.ScoochwormPartEntity
import dev.aaronhowser.mods.critterworks.registry.ModScoochwormAttachmentTypes
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.Mth
import org.joml.Vector3f
import software.bernie.geckolib.cache.`object`.BakedGeoModel
import software.bernie.geckolib.renderer.GeoRenderer
import software.bernie.geckolib.renderer.layer.GeoRenderLayer

class ScoochwormChunkLoaderLayer(
	renderer: GeoRenderer<ScoochwormPartEntity>
) : GeoRenderLayer<ScoochwormPartEntity>(renderer) {

	override fun render(
		poseStack: PoseStack,
		animatable: ScoochwormPartEntity,
		bakedModel: BakedGeoModel,
		renderType: RenderType?,
		bufferSource: MultiBufferSource,
		buffer: VertexConsumer?,
		partialTick: Float,
		packedLight: Int,
		packedOverlay: Int
	) {
		if (animatable.attachmentData.resolveType() != ModScoochwormAttachmentTypes.CHUNK_LOADER.get()) return

		val vertexConsumer = bufferSource.getBuffer(RENDER_TYPE)
		val animationTime = animatable.tickCount + partialTick
		val bob = Mth.sin(animationTime * BOB_SPEED) * BOB_HEIGHT

		poseStack.withPose {
			poseStack.translate(
				0.0,
				(DIAMOND_CENTER_Y + bob).toDouble(),
				0.0
			)

			poseStack.mulPose(Axis.YP.rotationDegrees(animationTime * ROTATION_SPEED))

			poseStack.translate(
				0.0,
				(-DIAMOND_CENTER_Y).toDouble(),
				0.0
			)

			val pose = poseStack.last()
			val bottom = Vector3f(0f, DIAMOND_BASE_Y, 0f)
			val top = Vector3f(0f, DIAMOND_BASE_Y + DIAMOND_HEIGHT, 0f)
			val north = Vector3f(0f, DIAMOND_CENTER_Y, -DIAMOND_RADIUS)
			val east = Vector3f(DIAMOND_RADIUS, DIAMOND_CENTER_Y, 0f)
			val south = Vector3f(0f, DIAMOND_CENTER_Y, DIAMOND_RADIUS)
			val west = Vector3f(-DIAMOND_RADIUS, DIAMOND_CENTER_Y, 0f)

			addFace(vertexConsumer, pose, bottom, west, north, BOTTOM_NORTH_WEST_NORMAL, packedLight)
			addFace(vertexConsumer, pose, bottom, north, east, BOTTOM_NORTH_EAST_NORMAL, packedLight)
			addFace(vertexConsumer, pose, bottom, east, south, BOTTOM_SOUTH_EAST_NORMAL, packedLight)
			addFace(vertexConsumer, pose, bottom, south, west, BOTTOM_SOUTH_WEST_NORMAL, packedLight)
			addFace(vertexConsumer, pose, top, north, west, TOP_NORTH_WEST_NORMAL, packedLight)
			addFace(vertexConsumer, pose, top, east, north, TOP_NORTH_EAST_NORMAL, packedLight)
			addFace(vertexConsumer, pose, top, south, east, TOP_SOUTH_EAST_NORMAL, packedLight)
			addFace(vertexConsumer, pose, top, west, south, TOP_SOUTH_WEST_NORMAL, packedLight)
		}
	}

	private fun addFace(
		vertexConsumer: VertexConsumer,
		pose: PoseStack.Pose,
		first: Vector3f,
		second: Vector3f,
		third: Vector3f,
		normal: Vector3f,
		light: Int
	) {
		addVertex(vertexConsumer, pose, first, 0f, 0f, light, normal)
		addVertex(vertexConsumer, pose, second, 1f, 0f, light, normal)
		addVertex(vertexConsumer, pose, third, 0.5f, 1f, light, normal)
		addVertex(vertexConsumer, pose, third, 0.5f, 1f, light, normal)
	}

	private fun addVertex(
		vertexConsumer: VertexConsumer,
		pose: PoseStack.Pose,
		position: Vector3f,
		u: Float,
		v: Float,
		light: Int,
		normal: Vector3f
	) {
		vertexConsumer.addVertex(pose.pose(), position.x, position.y, position.z)
			.setColor(0, 255, 255, 255)
			.setUv(u, v)
			.setOverlay(OverlayTexture.NO_OVERLAY)
			.setLight(light)
			.setNormal(pose, normal.x, normal.y, normal.z)
	}

	companion object {
		private const val BLOCK_PIXEL = 0.0625f
		private const val DIAMOND_BASE_Y = 17f * BLOCK_PIXEL
		private const val DIAMOND_HEIGHT = 6f * BLOCK_PIXEL
		private const val DIAMOND_WIDTH = 6f * BLOCK_PIXEL
		private const val DIAMOND_CENTER_Y = DIAMOND_BASE_Y + DIAMOND_HEIGHT / 2f
		private const val DIAMOND_RADIUS = DIAMOND_WIDTH / 2f
		private const val BOB_SPEED = 0.08f
		private const val BOB_HEIGHT = 0.5f * BLOCK_PIXEL
		private const val ROTATION_SPEED = 1.5f

		private val BOTTOM_NORTH_WEST_NORMAL = Vector3f(-0.6f, -0.55f, -0.6f).normalize()
		private val BOTTOM_NORTH_EAST_NORMAL = Vector3f(0.6f, -0.55f, -0.6f).normalize()
		private val BOTTOM_SOUTH_EAST_NORMAL = Vector3f(0.6f, -0.55f, 0.6f).normalize()
		private val BOTTOM_SOUTH_WEST_NORMAL = Vector3f(-0.6f, -0.55f, 0.6f).normalize()
		private val TOP_NORTH_WEST_NORMAL = Vector3f(-0.6f, 0.55f, -0.6f).normalize()
		private val TOP_NORTH_EAST_NORMAL = Vector3f(0.6f, 0.55f, -0.6f).normalize()
		private val TOP_SOUTH_EAST_NORMAL = Vector3f(0.6f, 0.55f, 0.6f).normalize()
		private val TOP_SOUTH_WEST_NORMAL = Vector3f(-0.6f, 0.55f, 0.6f).normalize()

		private val RENDER_TYPE = RenderType.entitySolid(
			ResourceLocation.withDefaultNamespace("textures/misc/white.png")
		)
	}
}