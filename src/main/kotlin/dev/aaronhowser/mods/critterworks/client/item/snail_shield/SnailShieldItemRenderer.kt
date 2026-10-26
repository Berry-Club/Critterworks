package dev.aaronhowser.mods.critterworks.client.item.snail_shield

import dev.aaronhowser.mods.critterworks.client.entity.snail_shield.SnailShieldModel
import dev.aaronhowser.mods.critterworks.item.SnailShieldItem
import software.bernie.geckolib.renderer.GeoItemRenderer

class SnailShieldItemRenderer : GeoItemRenderer<SnailShieldItem>(
	SnailShieldModel<SnailShieldItem>()
)