package dev.aaronhowser.mods.critterworks.item.arachnode

import dev.aaronhowser.mods.critterworks.handler.web.redstone.node.Arachnode
import dev.aaronhowser.mods.critterworks.handler.web.redstone.node.InvertingArachnode
import java.util.*

class InvertingArachnodeItem(properties: Properties) : ArachnodeItem(properties) {

	override fun createArachnode(context: ArachnodePlacementContext): Arachnode? {
		val arachnodeUuid = UUID.randomUUID()
		val ports = createDefaultPorts(context, arachnodeUuid) ?: return null

		return InvertingArachnode(
			arachnodeUuid,
			context.position,
			context.itemStack,
			listOf(ports.inputPort),
			listOf(ports.outputPort)
		)
	}
}