package dev.aaronhowser.mods.critterworks.client.critter_cage

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import dev.aaronhowser.mods.aaron.misc.AaronDsls.withPose
import dev.aaronhowser.mods.critterworks.entity.scoochworm.ScoochwormEntity
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.core.Direction
import net.minecraft.world.entity.LivingEntity
import org.joml.Quaternionf

object CritterCageEntityRenderer {

	fun render(
		entity: LivingEntity,
		down: Direction,
		forward: Direction,
		partialTick: Float,
		poseStack: PoseStack,
		bufferSource: MultiBufferSource,
		packedLight: Int
	) {
		val movementYaw = ScoochwormEntity.getMovementYaw(forward, down)

		if (entity is ScoochwormEntity) {
			setRenderRotation(entity, movementYaw)
			entity.supportDirection = down
			renderScoochworm(entity, movementYaw, partialTick, poseStack, bufferSource, packedLight)
			return
		}

		val renderYaw = if (down == Direction.UP) -movementYaw else movementYaw
		setRenderRotation(entity, renderYaw)

		val width = entity.bbWidth
		val height = entity.bbHeight
		val horizontalScale = if (width > 0f) MAXIMUM_WIDTH / width else 1f
		val verticalScale = if (height > 0f) MAXIMUM_HEIGHT / height else 1f
		val scale = minOf(horizontalScale, verticalScale, 1f)

		poseStack.withPose {
			poseStack.translate(0.5, 0.5, 0.5)
			poseStack.mulPose(getSurfaceRotation(down))
			poseStack.translate(0.0, -0.4, 0.0)
			poseStack.scale(scale, scale, scale)

			Minecraft.getInstance()
				.entityRenderDispatcher
				.getRenderer(entity)
				.render(
					entity,
					renderYaw,
					partialTick,
					poseStack,
					bufferSource,
					packedLight
				)
		}
	}

	private fun renderScoochworm(
		scoochworm: ScoochwormEntity,
		yaw: Float,
		partialTick: Float,
		poseStack: PoseStack,
		bufferSource: MultiBufferSource,
		packedLight: Int
	) {
		poseStack.withPose {
			poseStack.translate(0.5, 0.05, 0.5)

			Minecraft.getInstance()
				.entityRenderDispatcher
				.getRenderer(scoochworm)
				.render(
					scoochworm,
					yaw,
					partialTick,
					poseStack,
					bufferSource,
					packedLight
				)
		}
	}

	private fun setRenderRotation(entity: LivingEntity, yaw: Float) {
		entity.yRot = yaw
		entity.yRotO = yaw
		entity.yBodyRot = yaw
		entity.yBodyRotO = yaw
		entity.yHeadRot = yaw
		entity.yHeadRotO = yaw
	}

	private fun getSurfaceRotation(down: Direction): Quaternionf {
		return when (down) {
			Direction.DOWN -> Quaternionf()
			Direction.UP -> Axis.ZP.rotationDegrees(180f)
			Direction.NORTH -> Axis.XP.rotationDegrees(90f)
			Direction.SOUTH -> Axis.XP.rotationDegrees(-90f)
			Direction.WEST -> Axis.ZP.rotationDegrees(-90f)
			Direction.EAST -> Axis.ZP.rotationDegrees(90f)
		}
	}

	private const val MAXIMUM_WIDTH = 0.7f
	private const val MAXIMUM_HEIGHT = 0.8f
}