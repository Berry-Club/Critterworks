package dev.aaronhowser.mods.critterworks.client.model.entity

import dev.aaronhowser.mods.critterworks.Critterworks
import dev.aaronhowser.mods.critterworks.entity.scoochworm.ScoochwormEntity
import dev.aaronhowser.mods.critterworks.entity.scoochworm.ScoochwormPartEntity
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.Vec3
import software.bernie.geckolib.animation.AnimationState
import software.bernie.geckolib.model.DefaultedEntityGeoModel

class ScoochwormPartModel : DefaultedEntityGeoModel<ScoochwormPartEntity>(
	Critterworks.modResource("scoochworm/body")
) {

	override fun setCustomAnimations(
		animatable: ScoochwormPartEntity,
		instanceId: Long,
		animationState: AnimationState<ScoochwormPartEntity>
	) {
		val connection = animationProcessor.getBone("connection")
		val partInFront = animatable.getPartInFront()

		if (partInFront == null) {
			connection.isHidden = true
			return
		}

		connection.isHidden = false

		val partialTick = animationState.partialTick
		val currentPosition = getInterpolatedPosition(animatable, partialTick)
		val frontPosition = getInterpolatedPosition(partInFront, partialTick)
		val worldOffset = currentPosition.vectorTo(frontPosition)
		val interpolatedYaw = Mth.rotLerp(
			partialTick,
			animatable.yRotO,
			animatable.yRot
		)

		val localOffset = ScoochwormModel.getModelRotation(
			animatable.supportDirection,
			interpolatedYaw
		)
			.conjugate()
			.transform(worldOffset.toVector3f())

		val modelScale = MODEL_PIXELS_PER_BLOCK / ScoochwormEntity.SIZE
		connection.posX = -localOffset.x * modelScale / 2f
		connection.posY = localOffset.y * modelScale / 2f
		connection.posZ = localOffset.z * modelScale / 2f
	}

	private fun getInterpolatedPosition(entity: Entity, partialTick: Float): Vec3 {
		return Vec3(
			Mth.lerp(partialTick.toDouble(), entity.xOld, entity.x),
			Mth.lerp(partialTick.toDouble(), entity.yOld, entity.y),
			Mth.lerp(partialTick.toDouble(), entity.zOld, entity.z)
		)
	}

	companion object {
		private const val MODEL_PIXELS_PER_BLOCK = 16f
	}

}