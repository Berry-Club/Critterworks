package dev.aaronhowser.mods.critterworks.client.entity.punchprawn

import dev.aaronhowser.mods.critterworks.Critterworks
import dev.aaronhowser.mods.critterworks.entity.punchprawn.PunchprawnEntity
import net.minecraft.util.Mth
import software.bernie.geckolib.animation.AnimationState
import software.bernie.geckolib.constant.DataTickets
import software.bernie.geckolib.model.DefaultedEntityGeoModel

class PunchprawnModel : DefaultedEntityGeoModel<PunchprawnEntity>(
	Critterworks.modResource("punchprawn")
) {

	override fun setCustomAnimations(
		animatable: PunchprawnEntity,
		instanceId: Long,
		animationState: AnimationState<PunchprawnEntity>
	) {
		val entityData = animationState.getData(DataTickets.ENTITY_MODEL_DATA) ?: return
		val eyeYaw = entityData.netHeadYaw * Mth.DEG_TO_RAD

		animationProcessor.getBone("left_eye").rotY = eyeYaw
		animationProcessor.getBone("right_eye").rotY = eyeYaw
	}
}