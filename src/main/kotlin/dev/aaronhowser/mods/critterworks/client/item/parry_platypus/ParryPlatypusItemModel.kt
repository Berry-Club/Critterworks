package dev.aaronhowser.mods.critterworks.client.item.parry_platypus

import dev.aaronhowser.mods.critterworks.Critterworks
import dev.aaronhowser.mods.critterworks.item.ParryPlatypusItem
import software.bernie.geckolib.model.DefaultedGeoModel

class ParryPlatypusItemModel : DefaultedGeoModel<ParryPlatypusItem>(
	Critterworks.modResource("parry_platypus")
) {
	override fun subtype(): String = "entity"
}