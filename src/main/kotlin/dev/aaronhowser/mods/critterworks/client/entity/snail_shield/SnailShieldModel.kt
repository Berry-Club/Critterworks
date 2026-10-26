package dev.aaronhowser.mods.critterworks.client.entity.snail_shield

import dev.aaronhowser.mods.critterworks.Critterworks
import software.bernie.geckolib.animatable.GeoAnimatable
import software.bernie.geckolib.model.DefaultedGeoModel

class SnailShieldModel<T : GeoAnimatable> : DefaultedGeoModel<T>(
	Critterworks.modResource("snail_shield")
) {
	override fun subtype(): String = "entity"
}