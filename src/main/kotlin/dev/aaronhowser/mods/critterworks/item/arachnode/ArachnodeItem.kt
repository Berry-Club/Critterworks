package dev.aaronhowser.mods.critterworks.item.arachnode

import dev.aaronhowser.mods.critterworks.handler.web.redstone.RedstoneWebHandler
import dev.aaronhowser.mods.critterworks.handler.web.redstone.RedstoneWebLine
import dev.aaronhowser.mods.critterworks.handler.web.redstone.node.Arachnode
import dev.aaronhowser.mods.critterworks.handler.web.redstone.node.ArachnodePort
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.Item
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.gameevent.GameEvent
import net.minecraft.world.phys.Vec3
import java.util.*

abstract class ArachnodeItem(properties: Properties) : Item(properties) {

	abstract fun createArachnode(context: ArachnodePlacementContext): Arachnode?

	override fun useOn(context: UseOnContext): InteractionResult {
		val level = context.level
		if (level !is ServerLevel) return InteractionResult.SUCCESS

		val player = context.player ?: return InteractionResult.PASS
		val placed = RedstoneWebHandler.get(level).placeSurfaceArachnode(
			level,
			context.itemInHand,
			context.clickedPos,
			context.clickedFace,
			context.clickLocation,
			player.eyePosition
		)
		if (!placed) return InteractionResult.FAIL

		context.itemInHand.consume(1, player)
		level.gameEvent(player, GameEvent.BLOCK_PLACE, context.clickLocation)
		return InteractionResult.CONSUME
	}

	protected fun createDefaultPorts(
		context: ArachnodePlacementContext,
		arachnodeUuid: UUID
	): PlacedPorts? {
		val portDirections = getPortDirections(context) ?: return null

		val inputPort = ArachnodePort(
			UUID.randomUUID(),
			arachnodeUuid,
			context.position.add(portDirections.first.scale(HALF_DEVICE_LENGTH))
		)
		val outputPort = ArachnodePort(
			UUID.randomUUID(),
			arachnodeUuid,
			context.position.add(portDirections.second.scale(HALF_DEVICE_LENGTH))
		)

		return PlacedPorts(inputPort, outputPort)
	}

	private fun getPortDirections(context: ArachnodePlacementContext): Pair<Vec3, Vec3>? {
		if (context is ArachnodePlacementContext.OnLine) {
			return getLinePortDirections(context, context.originalLine)
		}
		if (context !is ArachnodePlacementContext.OnSurface) return null

		val surfaceNormal = context.mount.getSurfaceNormal()
		val awayFromPlayer = context.position.subtract(context.playerPosition)
		var outputDirection = awayFromPlayer.subtract(
			surfaceNormal.scale(awayFromPlayer.dot(surfaceNormal))
		)
		if (outputDirection.lengthSqr() < MIN_DIRECTION_LENGTH_SQUARED) {
			outputDirection = getFallbackSurfaceDirection(surfaceNormal)
		} else {
			outputDirection = outputDirection.normalize()
		}

		return Pair(outputDirection.scale(-1.0), outputDirection)
	}

	private fun getLinePortDirections(
		context: ArachnodePlacementContext,
		line: RedstoneWebLine
	): Pair<Vec3, Vec3>? {
		val directionToPlayer = context.playerPosition.subtract(context.position)
		if (directionToPlayer.lengthSqr() == 0.0) return null

		val directionToFirstNode = line.firstNode.position
			.subtract(context.position)
			.normalize()
		val directionToSecondNode = line.secondNode.position
			.subtract(context.position)
			.normalize()

		if (directionToFirstNode.dot(directionToPlayer)
			>= directionToSecondNode.dot(directionToPlayer)
		) return Pair(directionToFirstNode, directionToSecondNode)

		return Pair(directionToSecondNode, directionToFirstNode)
	}

	private fun getFallbackSurfaceDirection(surfaceNormal: Vec3): Vec3 {
		if (kotlin.math.abs(surfaceNormal.y) < 0.5) return Vec3(0.0, 1.0, 0.0)
		return Vec3(0.0, 0.0, -1.0)
	}

	protected class PlacedPorts(
		val inputPort: ArachnodePort,
		val outputPort: ArachnodePort
	)

	companion object {
		private const val HALF_DEVICE_LENGTH = 0.125
		private const val MIN_DIRECTION_LENGTH_SQUARED = 1.0e-6
	}
}