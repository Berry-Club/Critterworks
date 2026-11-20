package dev.aaronhowser.mods.critterworks.item

import dev.aaronhowser.mods.aaron.misc.AaronExtensions.status
import dev.aaronhowser.mods.critterworks.handler.web.common.node.WebLineAnchor
import dev.aaronhowser.mods.critterworks.handler.web.common.node.WebNode
import dev.aaronhowser.mods.critterworks.handler.web.redstone.RedstoneWebLine
import dev.aaronhowser.mods.critterworks.handler.web.redstone.interaction.RedstoneWebLineInteractionItem
import dev.aaronhowser.mods.critterworks.handler.web.redstone.interaction.RedstoneWebNodeInteractionItem
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack

class WebstoneProbeItem(properties: Properties) : Item(properties),
	RedstoneWebNodeInteractionItem,
	RedstoneWebLineInteractionItem {

	override fun interactWithRedstoneWebNode(
		level: ServerLevel,
		player: ServerPlayer,
		itemStack: ItemStack,
		node: WebNode
	) {
		var strongestPower = 0

		for (line in node.lines) {
			val redstoneLine = line as? RedstoneWebLine ?: continue
			strongestPower = maxOf(strongestPower, redstoneLine.power)
		}

		player.status(strongestPower.toString())
	}

	override fun interactWithRedstoneWebLine(
		level: ServerLevel,
		player: ServerPlayer,
		itemStack: ItemStack,
		line: RedstoneWebLine,
		target: WebLineAnchor
	) {
		player.status(line.power.toString())
	}

}