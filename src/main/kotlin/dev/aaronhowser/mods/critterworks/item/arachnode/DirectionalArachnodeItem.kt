package dev.aaronhowser.mods.critterworks.item.arachnode

import dev.aaronhowser.mods.critterworks.handler.web.redstone.node.Arachnode
import dev.aaronhowser.mods.critterworks.handler.web.redstone.node.ArachnodeMount
import dev.aaronhowser.mods.critterworks.handler.web.redstone.node.DirectionalArachnode
import java.util.*

class DirectionalArachnodeItem(properties: Properties) : ArachnodeItem(properties) {

	override fun createArachnode(context: ArachnodePlacementContext): Arachnode? {
		val arachnodeUuid = UUID.randomUUID()
		val ports = createDefaultPorts(context, arachnodeUuid) ?: return null
		val mount = when (context) {
			is ArachnodePlacementContext.OnLine -> ArachnodeMount.Line
			is ArachnodePlacementContext.OnSurface -> context.mount
		}

		return DirectionalArachnode(
			arachnodeUuid,
			context.position,
			context.itemStack,
			mount,
			listOf(ports.inputPort),
			listOf(ports.outputPort)
		)
	}
}