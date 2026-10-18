package dev.aaronhowser.mods.critterworks.handler.sparkbug

import net.minecraft.network.chat.Component

sealed interface PollenPointInteractionResult {
	data object Added : PollenPointInteractionResult
	data object Removed : PollenPointInteractionResult
	data class Failed(val message: Component) : PollenPointInteractionResult
}