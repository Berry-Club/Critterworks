package dev.aaronhowser.mods.critterworks.item.arachnode

import dev.aaronhowser.mods.critterworks.handler.web.redstone.node.Arachnode
import dev.aaronhowser.mods.critterworks.handler.web.redstone.node.InvertingArachnode
import java.util.*

class InvertingArachnodeItem(properties: Properties) : ArachnodeItem(properties) {

	override fun createArachnode(context: ArachnodePlacementContext): Arachnode? {
		val inputLineUuid = getInputLineUuid(context) ?: return null

		return InvertingArachnode(
			UUID.randomUUID(),
			context.position,
			context.itemStack,
			inputLineUuid
		)
	}
}