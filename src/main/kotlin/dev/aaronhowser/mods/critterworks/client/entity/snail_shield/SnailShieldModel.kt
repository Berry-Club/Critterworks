package dev.aaronhowser.mods.critterworks.client.entity.snail_shield

import dev.aaronhowser.mods.critterworks.Critterworks
import software.bernie.geckolib.model.DefaultedGeoModel

class SnailShieldModel : DefaultedGeoModel<SnailShieldRenderer.ShellAnimatable>(
	Critterworks.modResource("snail_shield")
) {
	override fun subtype(): String = "entity"
}