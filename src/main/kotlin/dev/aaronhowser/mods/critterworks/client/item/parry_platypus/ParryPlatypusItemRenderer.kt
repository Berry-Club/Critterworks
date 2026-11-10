package dev.aaronhowser.mods.critterworks.client.item.parry_platypus

import dev.aaronhowser.mods.critterworks.client.entity.parry_platypus.ParryPlatypusModel
import dev.aaronhowser.mods.critterworks.item.ParryPlatypusItem
import software.bernie.geckolib.renderer.GeoItemRenderer

class ParryPlatypusItemRenderer : GeoItemRenderer<ParryPlatypusItem>(
	ParryPlatypusModel()
)