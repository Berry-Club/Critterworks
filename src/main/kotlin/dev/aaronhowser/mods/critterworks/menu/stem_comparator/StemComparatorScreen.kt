package dev.aaronhowser.mods.critterworks.menu.stem_comparator

import dev.aaronhowser.mods.aaron.menu.BaseScreen
import dev.aaronhowser.mods.aaron.menu.textures.ScreenBackground
import dev.aaronhowser.mods.critterworks.Critterworks
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory

class StemComparatorScreen(menu: StemComparatorMenu, inventory: Inventory, title: Component) :
	BaseScreen<StemComparatorMenu>(menu, inventory, title) {

	override val background: ScreenBackground = BACKGROUND

	companion object {
		val BACKGROUND = ScreenBackground(Critterworks.modResource("textures/gui/stem_comparator.png"), 176, 166)
	}
}
