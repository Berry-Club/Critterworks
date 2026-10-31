package dev.aaronhowser.mods.critterworks.handler.web.redstone.power

import dev.aaronhowser.mods.critterworks.handler.web.common.node.WebBlockAnchor
import dev.aaronhowser.mods.critterworks.handler.web.common.node.WebLineAnchor
import dev.aaronhowser.mods.critterworks.handler.web.common.node.WebNode
import dev.aaronhowser.mods.critterworks.handler.web.redstone.RedstoneWebLine
import dev.aaronhowser.mods.critterworks.handler.web.redstone.node.Arachnode
import dev.aaronhowser.mods.critterworks.handler.web.redstone.node.ArachnodePort
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.RedStoneWireBlock
import java.util.*

class RedstoneWebPowerSolver(
	private val lines: Map<UUID, RedstoneWebLine>,
	private val arachnodes: Collection<Arachnode>
) {

	fun solve(level: ServerLevel): Map<UUID, Int> {
		if (lines.isEmpty()) return emptyMap()

		val currentPower = lines.values.associate { line -> line.uuid to line.power }

		var generatedPower = getGeneratedLinePower(level, currentPower)
		val generatedPowerHistory: MutableList<Map<UUID, Int>> = mutableListOf()
		generatedPowerHistory.add(generatedPower)

		var desiredPower = calculatePropagatedPower(level, generatedPower)
		val maximumPasses = maxOf(1, arachnodes.size + 1)

		for (pass in 0 until maximumPasses) {
			val nextGeneratedPower = getGeneratedLinePower(level, desiredPower)
			if (nextGeneratedPower == generatedPower) return desiredPower

			val repeatedStateIndex = generatedPowerHistory.indexOf(nextGeneratedPower)
			if (repeatedStateIndex >= 0) {
				val stableGeneratedPower = getStableGeneratedPower(
					generatedPowerHistory.subList(repeatedStateIndex, generatedPowerHistory.size)
				)
				return calculatePropagatedPower(level, stableGeneratedPower)
			}

			generatedPower = nextGeneratedPower
			generatedPowerHistory.add(generatedPower)
			desiredPower = calculatePropagatedPower(level, generatedPower)
		}

		val stableGeneratedPower = getStableGeneratedPower(generatedPowerHistory)
		return calculatePropagatedPower(level, stableGeneratedPower)
	}

	private fun calculatePropagatedPower(
		level: ServerLevel,
		generatedPower: Map<UUID, Int>
	): MutableMap<UUID, Int> {
		val desiredPower: MutableMap<UUID, Int> = mutableMapOf()
		val linesToCheck: ArrayDeque<RedstoneWebLine> = ArrayDeque()
		val blockInputPower: MutableMap<UUID, Int> = mutableMapOf()
		beginReadingInputs()

		return try {
			for (line in lines.values) {
				if (!line.isLoaded(level)) {
					desiredPower[line.uuid] = 0
					continue
				}

				val directPower = maxOf(
					getBlockInputPower(level, line.firstNode, blockInputPower),
					getBlockInputPower(level, line.secondNode, blockInputPower)
				)

				desiredPower[line.uuid] = directPower
				if (directPower > 0) linesToCheck.add(line)
			}

			for ((lineUuid, power) in generatedPower) {
				val line = lines[lineUuid] ?: continue
				if (!line.isLoaded(level)) continue

				val previousPower = desiredPower.getValue(lineUuid)
				if (previousPower >= power) continue

				desiredPower[lineUuid] = power
				linesToCheck.add(line)
			}

			while (linesToCheck.isNotEmpty()) {
				val line = linesToCheck.removeFirst()
				val linePower = desiredPower.getValue(line.uuid)

				for (poweredLine in getPoweredLines(line)) {
					if (!poweredLine.isLoaded(level)) continue

					val previousPower = desiredPower.getValue(poweredLine.uuid)
					if (previousPower >= linePower) continue

					desiredPower[poweredLine.uuid] = linePower
					linesToCheck.add(poweredLine)
				}
			}

			desiredPower
		} finally {
			endReadingInputs()
		}
	}

	private fun getGeneratedLinePower(
		level: ServerLevel,
		linePowers: Map<UUID, Int>
	): Map<UUID, Int> {
		val generatedPower: MutableMap<UUID, Int> = mutableMapOf()
		for (arachnode in arachnodes) {
			if (!arachnode.isLoaded(level)) continue

			val inputPowers: MutableMap<ArachnodePort, Int> = mutableMapOf()
			for (inputPort in arachnode.inputPorts) {
				var strongestPower = 0
				for (line in inputPort.lines) {
					if (line !is RedstoneWebLine || !line.isLoaded(level)) continue
					strongestPower = maxOf(
						strongestPower,
						linePowers.getOrDefault(line.uuid, 0)
					)
				}

				inputPowers[inputPort] = strongestPower
			}

			val outputPowers = arachnode.calculateOutputPowers(inputPowers)
			for ((outputPort, power) in outputPowers) {
				for (line in outputPort.lines) {
					if (line !is RedstoneWebLine || !line.isLoaded(level)) continue
					if (power <= generatedPower.getOrDefault(line.uuid, 0)) continue

					generatedPower[line.uuid] = power
				}
			}
		}

		return generatedPower
	}

	private fun getStableGeneratedPower(
		generatedPowerHistory: List<Map<UUID, Int>>
	): Map<UUID, Int> {
		if (generatedPowerHistory.isEmpty()) return emptyMap()

		val stableGeneratedPower: MutableMap<UUID, Int> = mutableMapOf()
		val firstState = generatedPowerHistory.first()
		for ((lineUuid, firstPower) in firstState) {
			val stable = generatedPowerHistory.all { state ->
				state.getOrDefault(lineUuid, 0) == firstPower
			}

			if (stable && firstPower > 0) stableGeneratedPower[lineUuid] = firstPower
		}

		return stableGeneratedPower
	}

	private fun getBlockInputPower(
		level: ServerLevel,
		node: WebNode,
		cachedPower: MutableMap<UUID, Int>
	): Int {
		if (node !is WebBlockAnchor) return 0

		val cached = cachedPower[node.uuid]
		if (cached != null) return cached
		if (!node.isLoaded(level)) return 0

		val blockPower = level.getSignal(node.blockPos, node.face.opposite)
		val signalPosition = node.blockPos.relative(node.face)
		val signalState = level.getBlockState(signalPosition)
		val dustPower = if (signalState.block is RedStoneWireBlock) {
			(signalState.getValue(RedStoneWireBlock.POWER) - 1).coerceAtLeast(0)
		} else {
			0
		}

		val power = maxOf(blockPower, dustPower).coerceIn(0, MAX_POWER)
		cachedPower[node.uuid] = power
		return power
	}

	private fun getPoweredLines(line: RedstoneWebLine): Set<RedstoneWebLine> {
		val connectedLines: MutableSet<RedstoneWebLine> = mutableSetOf()
		addLinesPoweredThroughNode(line.firstNode, connectedLines)
		addLinesPoweredThroughNode(line.secondNode, connectedLines)

		for (attachment in line.attachedAnchors) {
			connectedLines.addAll(attachment.anchor.lines.filterIsInstance<RedstoneWebLine>())
		}

		connectedLines.remove(line)
		return connectedLines
	}

	private fun addLinesPoweredThroughNode(
		node: WebNode,
		connectedLines: MutableSet<RedstoneWebLine>
	) {
		connectedLines.addAll(node.lines.filterIsInstance<RedstoneWebLine>())

		if (node !is WebLineAnchor) return

		val parentLine = lines[node.parentLineUuid] ?: return
		connectedLines.add(parentLine)
	}

	companion object {
		private const val MAX_POWER = 15
		private val inputReadingDepth: ThreadLocal<Int> = ThreadLocal.withInitial { 0 }

		fun isReadingInputs(): Boolean {
			return inputReadingDepth.get() > 0
		}

		private fun beginReadingInputs() {
			inputReadingDepth.set(inputReadingDepth.get() + 1)
		}

		private fun endReadingInputs() {
			val newDepth = inputReadingDepth.get() - 1
			if (newDepth <= 0) {
				inputReadingDepth.remove()
			} else {
				inputReadingDepth.set(newDepth)
			}
		}
	}
}