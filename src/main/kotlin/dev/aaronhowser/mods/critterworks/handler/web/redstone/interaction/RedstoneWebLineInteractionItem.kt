package dev.aaronhowser.mods.critterworks.handler.web.redstone.interaction

import dev.aaronhowser.mods.critterworks.handler.web.common.node.WebLineAnchor
import dev.aaronhowser.mods.critterworks.handler.web.redstone.RedstoneWebLine
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack

interface RedstoneWebLineInteractionItem {
	fun interactWithRedstoneWebLine(
		level: ServerLevel,
		player: ServerPlayer,
		itemStack: ItemStack,
		line: RedstoneWebLine,
		target: WebLineAnchor
	)
}