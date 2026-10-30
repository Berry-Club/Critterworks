package dev.aaronhowser.mods.critterworks.client.web

import com.mojang.blaze3d.vertex.PoseStack
import dev.aaronhowser.mods.aaron.misc.AaronDsls.withPose
import dev.aaronhowser.mods.critterworks.config.ClientConfig
import dev.aaronhowser.mods.critterworks.registry.ModItems
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.LevelRenderer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.core.BlockPos
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import org.joml.Matrix3f
import org.joml.Quaternionf

object DirectionalArachnodeRenderer {
	private val hoppingSpiderStack by lazy { ModItems.HOPPING_SPIDER.get().defaultInstance }

	fun renderAll(
		bufferSource: MultiBufferSource.BufferSource,
		level: Level,
		poseStack: PoseStack,
		cameraPosition: Vec3
	) {
		val minecraft = Minecraft.getInstance()
		val hoppingSpiderScale = ClientConfig.CONFIG.hoppingSpiderScale.get().toFloat()

		for (arachnode in ClientWebLines.getArachnodes()) {
			val outputDirection = arachnode.getDisplayDirection() ?: continue
			val spiderUp = getSpiderUp(outputDirection)
			val rotation = getTravelRotation(outputDirection, spiderUp)
			val light = LevelRenderer.getLightColor(level, BlockPos.containing(arachnode.position))

			poseStack.withPose {
				val displayPosition = arachnode.position.add(spiderUp.scale(DISTANCE_FROM_LINE))
				val relativePosition = displayPosition.subtract(cameraPosition)
				poseStack.translate(relativePosition.x, relativePosition.y, relativePosition.z)
				poseStack.mulPose(rotation)
				poseStack.scale(hoppingSpiderScale, hoppingSpiderScale, hoppingSpiderScale)

				minecraft.itemRenderer.renderStatic(
					hoppingSpiderStack,
					ItemDisplayContext.GROUND,
					light,
					OverlayTexture.NO_OVERLAY,
					poseStack,
					bufferSource,
					level,
					0
				)
			}
		}

		bufferSource.endBatch()
	}

	private fun getTravelRotation(direction: Vec3, spiderUp: Vec3): Quaternionf {
		val backward = direction.scale(-1.0)
		val right = spiderUp.cross(backward).normalize()
		val rotationMatrix = Matrix3f()
			.setColumn(0, right.toVector3f())
			.setColumn(1, spiderUp.toVector3f())
			.setColumn(2, backward.toVector3f())

		return Quaternionf().setFromNormalized(rotationMatrix)
	}

	private fun getSpiderUp(direction: Vec3): Vec3 {
		var spiderUp = UP.subtract(direction.scale(UP.dot(direction)))

		if (spiderUp.lengthSqr() < MIN_DIRECTION_LENGTH_SQUARED) {
			spiderUp = NORTH.subtract(direction.scale(NORTH.dot(direction)))
		}

		return spiderUp.normalize()
	}

	private const val DISTANCE_FROM_LINE = 0.04
	private const val MIN_DIRECTION_LENGTH_SQUARED = 1.0e-6

	private val UP = Vec3(0.0, 1.0, 0.0)
	private val NORTH = Vec3(0.0, 0.0, -1.0)
}