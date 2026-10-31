package dev.aaronhowser.mods.critterworks.handler.web.redstone.power

import dev.aaronhowser.mods.critterworks.handler.web.common.node.WebBlockAnchor
import dev.aaronhowser.mods.critterworks.handler.web.redstone.RedstoneWebLine
import dev.aaronhowser.mods.critterworks.handler.web.redstone.storage.RedstoneWebGraph
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.Blocks
import java.util.*

class RedstoneWebPowerController(
	private val graph: RedstoneWebGraph
) {

	/**
	 * Updates every line and returns the lines that changed.
	 * Power is cleared first when a signal gets weaker so nearby blocks do not keep it powered.
	 */
	fun updatePower(level: ServerLevel): Set<RedstoneWebLine> {
		val lines = graph.lines
		if (lines.isEmpty()) return emptySet()

		val changedLines = mutableSetOf<RedstoneWebLine>()
		val solver = RedstoneWebPowerSolver(lines, graph.arachnodes.values)

		var desiredPower = solver.solve(level)
		var hasFallingPower = false

		for (line in lines.values) {
			if (desiredPower.getValue(line.uuid) >= line.power) continue

			hasFallingPower = true
			break
		}

		if (hasFallingPower) {
			for (line in lines.values) {
				if (line.updatePower(0)) changedLines.add(line)
			}

			notifyConnectedBlocks(level, lines.values)
			desiredPower = solver.solve(level)
		}

		val poweredLines = mutableSetOf<RedstoneWebLine>()
		for (line in lines.values) {
			if (!line.updatePower(desiredPower.getValue(line.uuid))) continue

			changedLines.add(line)
			poweredLines.add(line)
		}

		notifyConnectedBlocks(level, poweredLines)
		return changedLines
	}

	fun notifyConnectedBlocks(
		level: ServerLevel,
		lines: Collection<RedstoneWebLine>
	) {
		val notifiedAnchors = mutableSetOf<UUID>()
		for (line in lines) {
			val firstNode = line.firstNode
			if (firstNode is WebBlockAnchor && notifiedAnchors.add(firstNode.uuid)) {
				notifyConnectedBlock(level, firstNode)
			}

			val secondNode = line.secondNode
			if (secondNode is WebBlockAnchor && notifiedAnchors.add(secondNode.uuid)) {
				notifyConnectedBlock(level, secondNode)
			}
		}
	}

	fun notifyConnectedBlocks(level: ServerLevel, line: RedstoneWebLine) {
		notifyConnectedBlocks(level, listOf(line))
	}

	private fun notifyConnectedBlock(
		level: ServerLevel,
		anchor: WebBlockAnchor
	) {
		val signalPosition = anchor.blockPos.relative(anchor.face)
		if (!anchor.isLoaded(level)) return

		level.neighborChanged(signalPosition, Blocks.AIR, anchor.blockPos)
		level.updateNeighborsAt(signalPosition, Blocks.AIR)
	}
}