package dev.aaronhowser.mods.critterworks.client.render.item

import dev.aaronhowser.mods.critterworks.client.model.item.HoppingSpiderItemModel
import dev.aaronhowser.mods.critterworks.item.HoppingSpiderItem
import dev.aaronhowser.mods.critterworks.item.component.HoppingSpiderAnimation
import dev.aaronhowser.mods.critterworks.registry.ModDataComponents
import software.bernie.geckolib.renderer.GeoItemRenderer

class HoppingSpiderItemRenderer : GeoItemRenderer<HoppingSpiderItem>(HoppingSpiderItemModel()) {

	override fun getInstanceId(animatable: HoppingSpiderItem): Long {
		val animation = currentItemStack.getOrDefault(
			ModDataComponents.HOPPING_SPIDER_ANIMATION,
			HoppingSpiderAnimation.IDLE
		)

		return animation.ordinal.toLong()
	}
}