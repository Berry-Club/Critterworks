package dev.aaronhowser.mods.critterworks.menu.lockbox

import dev.aaronhowser.mods.aaron.menu.textures.ScreenBackground
import dev.aaronhowser.mods.critterworks.Critterworks
import dev.aaronhowser.mods.critterworks.menu.CritterworksScreen
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory

class LockboxScreen(
	menu: LockboxMenu,
	playerInventory: Inventory,
	title: Component
) : CritterworksScreen<LockboxMenu>(menu, playerInventory, title) {

	override val background: ScreenBackground = BACKGROUND

	companion object {
		val BACKGROUND = ScreenBackground(Critterworks.modResource("textures/gui/lockbox.png"), 176, 182)
	}

}