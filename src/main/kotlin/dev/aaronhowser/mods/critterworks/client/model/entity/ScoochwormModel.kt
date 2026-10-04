package dev.aaronhowser.mods.critterworks.client.model.entity

import com.mojang.math.Axis
import dev.aaronhowser.mods.critterworks.Critterworks
import dev.aaronhowser.mods.critterworks.entity.ScoochwormEntity
import net.minecraft.core.Direction
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3
import org.joml.Quaternionf
import org.joml.Vector3f
import software.bernie.geckolib.animation.AnimationState
import software.bernie.geckolib.constant.DataTickets
import software.bernie.geckolib.model.DefaultedEntityGeoModel

class ScoochwormModel : DefaultedEntityGeoModel<ScoochwormEntity>(
	Critterworks.modResource("scoochworm/head")
) {

	override fun setCustomAnimations(
		animatable: ScoochwormEntity,
		instanceId: Long,
		animationState: AnimationState<ScoochwormEntity>
	) {
		val face = animationProcessor.getBone("face")
		val entityData = animationState.getData(DataTickets.ENTITY_MODEL_DATA) ?: return

		val partialTick = animationState.partialTick

		val bodyYaw = Mth.rotLerp(partialTick, animatable.yRotO, animatable.yRot)
		val headYaw = Mth.rotLerp(partialTick, animatable.yHeadRotO, animatable.yHeadRot)

		val worldLookDirection = Vec3.directionFromRotation(entityData.headPitch, headYaw)
		val localLookDirection = getLocalLookDirection(worldLookDirection, animatable.supportDirection, bodyYaw)

		val isAttachedToWall = animatable.supportDirection.axis != Direction.Axis.Y
		val horizontalLookDirection = if (isAttachedToWall) {
			localLookDirection.x
		} else {
			-localLookDirection.x
		}

		val verticalLookDirection = if (isAttachedToWall) {
			localLookDirection.y
		} else {
			-localLookDirection.y
		}

		face.posY = Mth.map(
			verticalLookDirection,
			-1f,
			1f,
			-3f,
			3f
		)

		face.posX = Mth.map(
			horizontalLookDirection,
			-1f,
			1f,
			-3f,
			3f
		)
	}

	private fun getLocalLookDirection(
		worldLookDirection: Vec3,
		supportDirection: Direction,
		bodyYaw: Float
	): Vector3f {
		val lookDirection = worldLookDirection.toVector3f()
		return getModelRotation(supportDirection, bodyYaw)
			.conjugate()
			.transform(lookDirection)
	}

	companion object {
		fun getModelRotation(
			supportDirection: Direction,
			bodyYaw: Float
		): Quaternionf {
			val modelRotation = when (supportDirection) {
				Direction.DOWN -> Axis.YP.rotationDegrees(0f)
				Direction.UP -> Axis.ZP.rotationDegrees(180f)
				Direction.NORTH -> Axis.XP.rotationDegrees(90f)
				Direction.SOUTH -> Axis.XP.rotationDegrees(-90f)
				Direction.WEST -> Axis.ZP.rotationDegrees(-90f)
				Direction.EAST -> Axis.ZP.rotationDegrees(90f)
			}

			val surfaceYaw = if (supportDirection == Direction.UP) -bodyYaw else bodyYaw
			modelRotation.mul(Axis.YP.rotationDegrees(180f - surfaceYaw))

			return modelRotation
		}
	}

}