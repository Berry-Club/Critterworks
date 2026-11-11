package dev.aaronhowser.mods.critterworks.client.entity.parry_platypus

import dev.aaronhowser.mods.critterworks.Critterworks
import dev.aaronhowser.mods.critterworks.entity.parry_platypus.ParryPlatypusEntity
import net.minecraft.util.Mth
import software.bernie.geckolib.animatable.GeoAnimatable
import software.bernie.geckolib.animation.AnimationState
import software.bernie.geckolib.constant.DataTickets
import software.bernie.geckolib.model.DefaultedGeoModel

class ParryPlatypusModel<T : GeoAnimatable> : DefaultedGeoModel<T>(
	Critterworks.modResource("parry_platypus")
) {
	override fun subtype(): String = "entity"

	override fun setCustomAnimations(
		animatable: T,
		instanceId: Long,
		animationState: AnimationState<T>
	) {
		if (animatable !is ParryPlatypusEntity) return

		val entityData = animationState.getData(DataTickets.ENTITY_MODEL_DATA) ?: return
		val head = animationProcessor.getBone("head")

		head.rotX = entityData.headPitch * Mth.DEG_TO_RAD
		head.rotY = entityData.netHeadYaw * Mth.DEG_TO_RAD
	}
}