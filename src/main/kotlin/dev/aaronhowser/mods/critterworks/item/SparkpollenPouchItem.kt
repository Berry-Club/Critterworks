package dev.aaronhowser.mods.critterworks.item

import dev.aaronhowser.mods.critterworks.registry.ModDataComponents
import net.minecraft.world.item.Item

class SparkpollenPouchItem(properties: Properties) : Item(properties) {

	companion object {
		val DEFAULT_PROPERTIES = {
			Properties()
				.stacksTo(1)
				.component(ModDataComponents.SPARKBUG_BUSH, 0)
		}
	}

}