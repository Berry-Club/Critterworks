package dev.aaronhowser.mods.critterworks.handler.web.redstone.interaction

import dev.aaronhowser.mods.critterworks.handler.web.common.node.WebNode
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack

interface RedstoneWebNodeInteractionItem {
	fun interactWithRedstoneWebNode(
		level: ServerLevel,
		player: ServerPlayer,
		itemStack: ItemStack,
		node: WebNode
	)
}