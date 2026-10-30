package dev.aaronhowser.mods.critterworks.item.arachnode

import dev.aaronhowser.mods.critterworks.handler.web.redstone.node.Arachnode
import dev.aaronhowser.mods.critterworks.handler.web.redstone.node.DirectionalArachnode
import java.util.*

class DirectionalArachnodeItem(properties: Properties) : ArachnodeItem(properties) {

	override fun createArachnode(context: ArachnodePlacementContext): Arachnode? {
		val line = context.originalLine
		val directionToPlayer = context.playerPosition.subtract(context.position)
		if (directionToPlayer.lengthSqr() == 0.0) return null

		val directionToFirstNode = line.firstNode.position
			.subtract(context.position)
			.normalize()
		val directionToSecondNode = line.secondNode.position
			.subtract(context.position)
			.normalize()
		val inputLineUuid = if (directionToFirstNode.dot(directionToPlayer)
			>= directionToSecondNode.dot(directionToPlayer)
		) {
			context.firstLineUuid
		} else {
			context.secondLineUuid
		}

		return DirectionalArachnode(
			UUID.randomUUID(),
			context.position,
			context.itemStack,
			inputLineUuid
		)
	}
}