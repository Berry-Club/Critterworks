package dev.aaronhowser.mods.critterworks.client.entity.parry_platypus

import dev.aaronhowser.mods.critterworks.Critterworks
import software.bernie.geckolib.animatable.GeoAnimatable
import software.bernie.geckolib.model.DefaultedGeoModel

class ParryPlatypusModel<T : GeoAnimatable> : DefaultedGeoModel<T>(
	Critterworks.modResource("parry_platypus")
) {
	override fun subtype(): String = "entity"
}