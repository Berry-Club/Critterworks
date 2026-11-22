package dev.aaronhowser.mods.critterworks.client.item.directional_arachnode

import dev.aaronhowser.mods.critterworks.item.arachnode.DirectionalArachnodeItem
import software.bernie.geckolib.renderer.GeoItemRenderer

class DirectionalArachnodeItemRenderer : GeoItemRenderer<DirectionalArachnodeItem>(
	DirectionalArachnodeItemModel()
)