package dev.aaronhowser.mods.critterworks.handler.web.redstone.power

import dev.aaronhowser.mods.critterworks.handler.web.common.node.WebBlockAnchor
import dev.aaronhowser.mods.critterworks.handler.web.common.node.WebNode
import dev.aaronhowser.mods.critterworks.handler.web.redstone.RedstoneWebLine
import dev.aaronhowser.mods.critterworks.handler.web.redstone.storage.RedstoneWebGraph
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.Blocks
import java.util.*

class RedstoneWebPowerController(
	private val topology: RedstoneWebGraph
) {

	fun updatePower(level: ServerLevel): Set<RedstoneWebLine> {
		val lines = topology.lines
		if (lines.isEmpty()) return emptySet()

		val changedLines: MutableSet<RedstoneWebLine> = mutableSetOf()
		val solver = RedstoneWebPowerSolver(lines, topology.arachnodes.values)
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

		val poweredLines: MutableSet<RedstoneWebLine> = mutableSetOf()
		for (line in lines.values) {
			if (!line.updatePower(desiredPower.getValue(line.uuid))) continue

			changedLines.add(line)
			poweredLines.add(line)
		}

		notifyConnectedBlocks(level, poweredLines)
		return changedLines
	}

	fun notifyConnectedBlocks(level: ServerLevel, lines: Collection<RedstoneWebLine>) {
		val updatedNodes: MutableSet<UUID> = mutableSetOf()
		for (line in lines) {
			notifyConnectedBlock(level, line.firstNode, updatedNodes)
			notifyConnectedBlock(level, line.secondNode, updatedNodes)
		}
	}

	fun notifyConnectedBlocks(level: ServerLevel, line: RedstoneWebLine) {
		val updatedNodes: MutableSet<UUID> = mutableSetOf()
		notifyConnectedBlock(level, line.firstNode, updatedNodes)
		notifyConnectedBlock(level, line.secondNode, updatedNodes)
	}

	private fun notifyConnectedBlock(
		level: ServerLevel,
		node: WebNode,
		updatedNodes: MutableSet<UUID>
	) {
		if (node !is WebBlockAnchor) return
		if (!updatedNodes.add(node.uuid)) return

		val signalPosition = node.blockPos.relative(node.face)
		if (!node.isLoaded(level)) return

		level.neighborChanged(signalPosition, Blocks.AIR, node.blockPos)
		level.updateNeighborsAt(signalPosition, Blocks.AIR)
	}
}