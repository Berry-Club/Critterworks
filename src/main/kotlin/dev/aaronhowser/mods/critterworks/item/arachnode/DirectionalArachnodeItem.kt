package dev.aaronhowser.mods.critterworks.item.arachnode

import dev.aaronhowser.mods.critterworks.handler.web.redstone.node.Arachnode
import dev.aaronhowser.mods.critterworks.handler.web.redstone.node.DirectionalArachnode
import java.util.*

class DirectionalArachnodeItem(properties: Properties) : ArachnodeItem(properties) {

	override fun createArachnode(context: ArachnodePlacementContext): Arachnode? {
		val inputLineUuid = getInputLineUuid(context) ?: return null

		return DirectionalArachnode(
			UUID.randomUUID(),
			context.position,
			context.itemStack,
			inputLineUuid
		)
	}
}