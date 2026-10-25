package dev.aaronhowser.mods.critterworks.client.entity.evil_snail

import dev.aaronhowser.mods.critterworks.Critterworks
import software.bernie.geckolib.model.DefaultedGeoModel

class EvilSnailModel : DefaultedGeoModel<EvilSnailRenderer.EvilSnailAnimatable>(
	Critterworks.modResource("evil_snail")
) {
	override fun subtype(): String = "entity"
}