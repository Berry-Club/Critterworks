package dev.aaronhowser.mods.critterworks.client.render.entity

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import dev.aaronhowser.mods.critterworks.client.model.entity.ScoochwormModel
import dev.aaronhowser.mods.critterworks.entity.ScoochwormEntity
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.core.Direction
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.Mth
import software.bernie.geckolib.renderer.GeoEntityRenderer

class ScoochwormRenderer(
	context: EntityRendererProvider.Context
) : GeoEntityRenderer<ScoochwormEntity>(context, ScoochwormModel()) {

	init {
		withScale(ScoochwormEntity.SIZE)
	}

	override fun getTextureLocation(animatable: ScoochwormEntity): ResourceLocation {
		return animatable.color.headTexture
	}

	override fun applyRotations(
		animatable: ScoochwormEntity,
		poseStack: PoseStack,
		ageInTicks: Float,
		rotationYaw: Float,
		partialTick: Float,
		nativeScale: Float
	) {
		val interpolatedYaw = Mth.rotLerp(
			partialTick,
			animatable.yRotO,
			animatable.yRot
		)

		applyRotations(
			poseStack,
			animatable.supportDirection,
			interpolatedYaw
		)
	}

	companion object {
		private const val ROTATION_CENTER = ScoochwormEntity.SIZE / 2.0

		fun applyRotations(
			poseStack: PoseStack,
			bottom: Direction,
			yaw: Float
		) {
			poseStack.translate(0.0, ROTATION_CENTER, 0.0)

			when (bottom) {
				Direction.DOWN -> Unit
				Direction.UP -> poseStack.mulPose(Axis.ZP.rotationDegrees(180f))
				Direction.NORTH -> poseStack.mulPose(Axis.XP.rotationDegrees(90f))
				Direction.SOUTH -> poseStack.mulPose(Axis.XP.rotationDegrees(-90f))
				Direction.WEST -> poseStack.mulPose(Axis.ZP.rotationDegrees(-90f))
				Direction.EAST -> poseStack.mulPose(Axis.ZP.rotationDegrees(90f))
			}

			val surfaceYaw = if (bottom == Direction.UP) -yaw else yaw
			poseStack.mulPose(Axis.YP.rotationDegrees(180f - surfaceYaw))
			poseStack.translate(0.0, -ROTATION_CENTER, 0.0)
		}

	}
}