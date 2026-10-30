package dev.aaronhowser.mods.critterworks.item.arachnode

import dev.aaronhowser.mods.critterworks.handler.web.redstone.node.Arachnode
import net.minecraft.world.item.Item
import java.util.*

abstract class ArachnodeItem(properties: Properties) : Item(properties) {

	abstract fun createArachnode(context: ArachnodePlacementContext): Arachnode?

	protected fun getInputLineUuid(context: ArachnodePlacementContext): UUID? {
		val line = context.originalLine
		val directionToPlayer = context.playerPosition.subtract(context.position)
		if (directionToPlayer.lengthSqr() == 0.0) return null

		val directionToFirstNode = line.firstNode.position
			.subtract(context.position)
			.normalize()
		val directionToSecondNode = line.secondNode.position
			.subtract(context.position)
			.normalize()

		return if (directionToFirstNode.dot(directionToPlayer)
			>= directionToSecondNode.dot(directionToPlayer)
		) {
			context.firstLineUuid
		} else {
			context.secondLineUuid
		}
	}
}