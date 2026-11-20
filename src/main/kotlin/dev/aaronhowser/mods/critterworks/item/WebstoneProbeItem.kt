package dev.aaronhowser.mods.critterworks.item

import dev.aaronhowser.mods.aaron.misc.AaronExtensions.status
import dev.aaronhowser.mods.critterworks.handler.web.common.node.WebNode
import dev.aaronhowser.mods.critterworks.handler.web.redstone.RedstoneWebLine
import dev.aaronhowser.mods.critterworks.handler.web.redstone.interaction.RedstoneWebLineInteractionContext
import dev.aaronhowser.mods.critterworks.handler.web.redstone.interaction.RedstoneWebLineInteractionItem
import dev.aaronhowser.mods.critterworks.handler.web.redstone.interaction.RedstoneWebNodeInteractionContext
import dev.aaronhowser.mods.critterworks.handler.web.redstone.interaction.RedstoneWebNodeInteractionItem
import net.minecraft.world.item.Item

class WebstoneProbeItem(properties: Properties) : Item(properties),
	RedstoneWebNodeInteractionItem,
	RedstoneWebLineInteractionItem {

	override fun interactWithRedstoneWebNode(context: RedstoneWebNodeInteractionContext) {
		context.player.status("Redstone strength: ${getRedstoneStrength(context.node)}")
	}

	override fun interactWithRedstoneWebLine(context: RedstoneWebLineInteractionContext) {
		context.player.status("Redstone strength: ${context.line.power}")
	}

	private fun getRedstoneStrength(node: WebNode): Int {
		var strongestPower = 0

		for (line in node.lines) {
			val redstoneLine = line as? RedstoneWebLine ?: continue
			strongestPower = maxOf(strongestPower, redstoneLine.power)
		}

		return strongestPower
	}
}