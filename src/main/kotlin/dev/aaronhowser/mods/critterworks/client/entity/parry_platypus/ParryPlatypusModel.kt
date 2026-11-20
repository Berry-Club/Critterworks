package dev.aaronhowser.mods.critterworks.client.entity.parry_platypus

import dev.aaronhowser.mods.critterworks.Critterworks
import dev.aaronhowser.mods.critterworks.entity.parry_platypus.ParryPlatypusEntity
import software.bernie.geckolib.animation.AnimationState
import software.bernie.geckolib.model.DefaultedEntityGeoModel

class ParryPlatypusModel : DefaultedEntityGeoModel<ParryPlatypusEntity>(
	Critterworks.modResource("parry_platypus"),
	"head"
) {

	override fun setCustomAnimations(
		animatable: ParryPlatypusEntity,
		instanceId: Long,
		animationState: AnimationState<ParryPlatypusEntity>
	) {
		super.setCustomAnimations(animatable, instanceId, animationState)

		if (animatable.isBaby) {
			val head = animationProcessor.getBone(headBone)
			head.updateScale(1.5f, 1.5f, 1.5f)
		}
	}

}