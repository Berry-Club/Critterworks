package dev.aaronhowser.mods.critterworks.item.arachnode

import dev.aaronhowser.mods.critterworks.handler.web.redstone.node.Arachnode
import dev.aaronhowser.mods.critterworks.handler.web.redstone.node.ArachnodePort
import dev.aaronhowser.mods.critterworks.handler.web.redstone.node.ArachnodePortRole
import net.minecraft.world.item.Item
import net.minecraft.world.phys.Vec3
import java.util.*

abstract class ArachnodeItem(properties: Properties) : Item(properties) {

	abstract fun createArachnode(context: ArachnodePlacementContext): Arachnode?

	protected fun createDefaultPorts(
		context: ArachnodePlacementContext,
		arachnodeUuid: UUID
	): PlacedPorts? {
		val line = context.originalLine
		val directionToPlayer = context.playerPosition.subtract(context.position)
		if (directionToPlayer.lengthSqr() == 0.0) return null

		val directionToFirstNode = line.firstNode.position
			.subtract(context.position)
			.normalize()
		val directionToSecondNode = line.secondNode.position
			.subtract(context.position)
			.normalize()

		val inputDirection: Vec3
		val outputDirection: Vec3
		if (directionToFirstNode.dot(directionToPlayer)
			>= directionToSecondNode.dot(directionToPlayer)
		) {
			inputDirection = directionToFirstNode
			outputDirection = directionToSecondNode
		} else {
			inputDirection = directionToSecondNode
			outputDirection = directionToFirstNode
		}

		val inputPort = ArachnodePort(
			UUID.randomUUID(),
			arachnodeUuid,
			ArachnodePortRole.INPUT,
			0,
			context.position.add(inputDirection.scale(HALF_DEVICE_LENGTH))
		)
		val outputPort = ArachnodePort(
			UUID.randomUUID(),
			arachnodeUuid,
			ArachnodePortRole.OUTPUT,
			0,
			context.position.add(outputDirection.scale(HALF_DEVICE_LENGTH))
		)

		return PlacedPorts(inputPort, outputPort)
	}

	protected class PlacedPorts(
		val inputPort: ArachnodePort,
		val outputPort: ArachnodePort
	)

	companion object {
		private const val HALF_DEVICE_LENGTH = 0.125
	}
}