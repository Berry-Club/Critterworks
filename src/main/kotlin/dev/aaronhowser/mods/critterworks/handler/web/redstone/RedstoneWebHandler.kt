package dev.aaronhowser.mods.critterworks.handler.web.redstone

import dev.aaronhowser.mods.aaron.packet.AaronPacket
import dev.aaronhowser.mods.aaron.scheduler.SchedulerExtensions.scheduleTaskInTicks
import dev.aaronhowser.mods.critterworks.handler.web.WebLineType
import dev.aaronhowser.mods.critterworks.handler.web.common.line.WebLine
import dev.aaronhowser.mods.critterworks.handler.web.common.line.WebLineData
import dev.aaronhowser.mods.critterworks.handler.web.common.line.WebLineInvalidation
import dev.aaronhowser.mods.critterworks.handler.web.common.line.WebLineInvalidationReason
import dev.aaronhowser.mods.critterworks.handler.web.common.node.WebBlockAnchor
import dev.aaronhowser.mods.critterworks.handler.web.common.node.WebLineAnchor
import dev.aaronhowser.mods.critterworks.handler.web.common.node.WebNode
import dev.aaronhowser.mods.critterworks.handler.web.redstone.node.Arachnode
import dev.aaronhowser.mods.critterworks.item.arachnode.ArachnodeItem
import dev.aaronhowser.mods.critterworks.item.arachnode.ArachnodePlacementContext
import dev.aaronhowser.mods.critterworks.packet.server_to_client.AddWebLinesPacket
import dev.aaronhowser.mods.critterworks.packet.server_to_client.RemoveWebLinePacket
import dev.aaronhowser.mods.critterworks.packet.server_to_client.SyncRedstoneWebPowerPacket
import dev.aaronhowser.mods.critterworks.registry.ModSoundEvents
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtOps
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundSource
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.RedStoneWireBlock
import net.minecraft.world.level.saveddata.SavedData
import net.minecraft.world.phys.Vec3
import java.util.*

class RedstoneWebHandler : SavedData() {

	private val nodes: MutableMap<UUID, WebNode> = mutableMapOf()
	private val lines: MutableMap<UUID, RedstoneWebLine> = mutableMapOf()

	private val lineUuidsByChunk: MutableMap<ChunkPos, MutableSet<UUID>> = mutableMapOf()
	private val lineUuidsBySignalPosition: MutableMap<BlockPos, MutableSet<UUID>> = mutableMapOf()
	private val chunksToValidate: MutableSet<ChunkPos> = mutableSetOf()

	@Synchronized
	fun addLine(level: ServerLevel, line: RedstoneWebLine) {
		val firstNode = getCanonicalNode(line.firstNode)
		val secondNode = getCanonicalNode(line.secondNode)
		val storedLine = if (firstNode === line.firstNode && secondNode === line.secondNode) {
			line
		} else {
			RedstoneWebLine(line.uuid, firstNode, secondNode, line.power)
		}

		val previousLine = lines[storedLine.uuid]
		if (previousLine != null) {
			removeLineReferences(previousLine)
			updateBlockAnchors(level, previousLine)
		}

		nodes[firstNode.uuid] = firstNode
		nodes[secondNode.uuid] = secondNode
		lines[storedLine.uuid] = storedLine
		addLineReferences(storedLine)
		setDirty()

		val changedLines = refreshLinePower(level)
		sendToNearbyPlayers(
			level,
			storedLine,
			AddWebLinesPacket.fromLines(WebLineType.REDSTONE, listOf(storedLine))
		)

		val linesToSynchronize: MutableSet<RedstoneWebLine> = changedLines.toMutableSet()
		linesToSynchronize.add(storedLine)
		synchronizePower(level, linesToSynchronize)
	}

	@Synchronized
	fun getNode(uuid: UUID): WebNode? {
		return nodes[uuid]
	}

	@Synchronized
	fun getCanonicalNode(node: WebNode): WebNode {
		return nodes[node.uuid] ?: node
	}

	@Synchronized
	fun getLine(uuid: UUID): RedstoneWebLine? {
		return lines[uuid]
	}

	@Synchronized
	fun getPower(position: BlockPos, direction: Direction): Int {
		val lineUuids = lineUuidsBySignalPosition[position] ?: return 0
		var strongestPower = 0

		for (lineUuid in lineUuids) {
			val line = lines[lineUuid] ?: continue
			if (!line.connectsTo(position, direction)) continue

			strongestPower = maxOf(strongestPower, line.power)
			if (strongestPower >= MAX_POWER) return MAX_POWER
		}

		return strongestPower
	}

	@Synchronized
	fun removeLine(level: ServerLevel, uuid: UUID): RedstoneWebLine? {
		val line = lines[uuid] ?: return null
		removeStoredLine(level, line)
		chunksToValidate.addAll(line.intersectedChunkPositions)
		setDirty()
		return line
	}

	@Synchronized
	fun addArachnode(
		level: ServerLevel,
		lineUuid: UUID,
		itemStack: ItemStack,
		position: Vec3,
		playerPosition: Vec3
	): Boolean {
		val line = lines[lineUuid] ?: return false
		if (isNearExistingNode(line, position)) return false

		val arachnodeItem = itemStack.item as? ArachnodeItem ?: return false
		val firstLineUuid = UUID.randomUUID()
		val secondLineUuid = UUID.randomUUID()
		val placementContext = ArachnodePlacementContext(
			line,
			firstLineUuid,
			secondLineUuid,
			position,
			playerPosition,
			itemStack.copyWithCount(1)
		)
		val arachnode = arachnodeItem.createArachnode(placementContext) ?: return false

		val firstSegment = RedstoneWebLine(
			firstLineUuid,
			line.firstNode,
			arachnode,
			line.power
		)
		val secondSegment = RedstoneWebLine(
			secondLineUuid,
			arachnode,
			line.secondNode,
			line.power
		)

		val affectedLines = getLinesAttachedTo(line)
		synchronizeReplacementRemovals(level, listOf(line), affectedLines)

		for (attachment in line.attachedAnchors) {
			val parentLine = if (attachment.distanceToFirstNode
				<= line.firstNode.position.distanceTo(arachnode.position)
			) {
				firstSegment
			} else {
				secondSegment
			}

			attachment.anchor.parentLineUuid = parentLine.uuid
		}

		removeLineForReplacement(level, line)
		addLineForReplacement(firstSegment)
		addLineForReplacement(secondSegment)
		finishLineReplacement(level, listOf(firstSegment, secondSegment), affectedLines)
		return true
	}

	@Synchronized
	fun removeArachnode(
		level: ServerLevel,
		nodeUuid: UUID
	): Arachnode? {
		val arachnode = nodes[nodeUuid] as? Arachnode ?: return null
		val connectedLines: MutableList<RedstoneWebLine> = mutableListOf()
		for (line in arachnode.lines) {
			if (line is RedstoneWebLine) connectedLines.add(line)
		}
		if (connectedLines.size != 2) return null

		val firstLine = connectedLines[0]
		val secondLine = connectedLines[1]
		val firstNode = getOtherNode(firstLine, arachnode) ?: return null
		val secondNode = getOtherNode(secondLine, arachnode) ?: return null
		val fusedLine = RedstoneWebLine(
			UUID.randomUUID(),
			firstNode,
			secondNode,
			maxOf(firstLine.power, secondLine.power)
		)

		val replacedLines = listOf(firstLine, secondLine)
		val affectedLines = getLinesAttachedTo(replacedLines)
		synchronizeReplacementRemovals(level, replacedLines, affectedLines)

		for (line in replacedLines) {
			for (attachment in line.attachedAnchors) {
				attachment.anchor.parentLineUuid = fusedLine.uuid
			}
		}

		removeLineForReplacement(level, firstLine)
		removeLineForReplacement(level, secondLine)
		addLineForReplacement(fusedLine)
		finishLineReplacement(level, listOf(fusedLine), affectedLines)
		return arachnode
	}

	@Synchronized
	fun syncChunk(player: ServerPlayer, chunkPos: ChunkPos) {
		val nearbyLines = getLinesWithEndpointInChunk(chunkPos)
		if (nearbyLines.isEmpty()) return

		AddWebLinesPacket
			.fromLines(WebLineType.REDSTONE, nearbyLines)
			.messagePlayer(player)
		synchronizePower(player, nearbyLines)
	}

	@Synchronized
	fun forgetChunk(player: ServerPlayer, chunkPos: ChunkPos) {
		val level = player.serverLevel()

		for (line in getLinesWithEndpointInChunk(chunkPos)) {
			var stillTrackingLine = false

			for (endpointChunk in line.getEndpointChunkPositions()) {
				if (endpointChunk == chunkPos) continue

				val trackingPlayers = level.chunkSource.chunkMap.getPlayers(endpointChunk, false)
				if (player !in trackingPlayers) continue

				stillTrackingLine = true
				break
			}

			if (!stillTrackingLine) {
				RemoveWebLinePacket(WebLineType.REDSTONE, line.uuid).messagePlayer(player)
			}
		}
	}

	@Synchronized
	fun markForValidation(blockPos: BlockPos) {
		chunksToValidate.add(ChunkPos(blockPos))
	}

	@Synchronized
	fun tick(level: ServerLevel) {
		val changedLines = refreshLinePower(level)
		synchronizePower(level, changedLines)
	}

	private fun refreshLinePower(level: ServerLevel): Set<RedstoneWebLine> {
		val changedLines: MutableSet<RedstoneWebLine> = mutableSetOf()
		val currentPower: MutableMap<UUID, Int> = mutableMapOf()
		for (line in lines.values) {
			currentPower[line.uuid] = line.power
		}
		val initialGeneratedPower = getGeneratedLinePower(currentPower)
		var desiredPower = calculateDesiredPower(level, initialGeneratedPower)
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

			updateBlockAnchors(level, lines.values)
			desiredPower = calculateDesiredPower(level, initialGeneratedPower)
		}

		val poweredLines: MutableSet<RedstoneWebLine> = mutableSetOf()
		for (line in lines.values) {
			if (!line.updatePower(desiredPower.getValue(line.uuid))) continue

			changedLines.add(line)
			poweredLines.add(line)
		}

		updateBlockAnchors(level, poweredLines)
		return changedLines
	}

	private fun calculateDesiredPower(
		level: ServerLevel,
		initialGeneratedPower: Map<UUID, Int>
	): MutableMap<UUID, Int> {
		var generatedPower = initialGeneratedPower
		val seenGeneratedPower: MutableSet<Map<UUID, Int>> = mutableSetOf()
		seenGeneratedPower.add(generatedPower)

		val firstDesiredPower = calculatePropagatedPower(level, generatedPower)
		var desiredPower = firstDesiredPower

		while (true) {
			val nextGeneratedPower = getGeneratedLinePower(desiredPower)
			if (nextGeneratedPower == generatedPower) return desiredPower
			if (!seenGeneratedPower.add(nextGeneratedPower)) return firstDesiredPower

			generatedPower = nextGeneratedPower
			desiredPower = calculatePropagatedPower(level, generatedPower)
		}
	}

	private fun calculatePropagatedPower(
		level: ServerLevel,
		generatedPower: Map<UUID, Int>
	): MutableMap<UUID, Int> {
		val desiredPower: MutableMap<UUID, Int> = mutableMapOf()
		val linesToCheck: ArrayDeque<RedstoneWebLine> = ArrayDeque()
		val previousSuppression = suppressWebstonePower
		suppressWebstonePower = true

		return try {
			for (line in lines.values) {
				val directPower = maxOf(
					getBlockInputPower(level, line.firstNode),
					getBlockInputPower(level, line.secondNode)
				)
				desiredPower[line.uuid] = directPower
				if (directPower > 0) linesToCheck.add(line)
			}

			for ((lineUuid, power) in generatedPower) {
				val line = lines[lineUuid] ?: continue
				val previousPower = desiredPower.getValue(lineUuid)
				if (previousPower >= power) continue

				desiredPower[lineUuid] = power
				linesToCheck.add(line)
			}

			while (linesToCheck.isNotEmpty()) {
				val line = linesToCheck.removeFirst()
				val linePower = desiredPower.getValue(line.uuid)

				for (poweredLine in getPoweredLines(line)) {
					val previousPower = desiredPower.getValue(poweredLine.uuid)
					if (previousPower >= linePower) continue

					desiredPower[poweredLine.uuid] = linePower
					linesToCheck.add(poweredLine)
				}
			}

			desiredPower
		} finally {
			suppressWebstonePower = previousSuppression
		}
	}

	private fun getGeneratedLinePower(linePowers: Map<UUID, Int>): Map<UUID, Int> {
		val generatedPower: MutableMap<UUID, Int> = mutableMapOf()

		for (node in nodes.values) {
			if (node !is Arachnode) continue

			for (line in node.lines) {
				if (line !is RedstoneWebLine) continue

				val power = node.getGeneratedPower(line, linePowers)
				if (power <= generatedPower.getOrDefault(line.uuid, 0)) continue

				generatedPower[line.uuid] = power
			}
		}

		return generatedPower
	}

	private fun getBlockInputPower(
		level: ServerLevel,
		node: WebNode
	): Int {
		if (node !is WebBlockAnchor) return 0
		if (!node.isLoaded(level)) return 0

		val blockPower = level.getSignal(node.blockPos, node.face.opposite)
		val signalPosition = node.blockPos.relative(node.face)
		val signalState = level.getBlockState(signalPosition)
		val dustPower = if (signalState.block is RedStoneWireBlock) {
			(signalState.getValue(RedStoneWireBlock.POWER) - 1).coerceAtLeast(0)
		} else {
			0
		}

		return maxOf(blockPower, dustPower).coerceIn(0, MAX_POWER)
	}

	@Synchronized
	fun validateChangedChunks(level: ServerLevel) {
		if (chunksToValidate.isEmpty()) return

		val invalidLines = getInvalidLines(level, getLinesNearChangedChunks())
		if (invalidLines.isEmpty()) return

		for (line in invalidLines.keys) {
			removeStoredLine(level, line)
		}

		setDirty()
	}

	private fun getInvalidLines(
		level: ServerLevel,
		lineUuids: Set<UUID>
	): Map<RedstoneWebLine, WebLineInvalidation> {
		val commonLines: Map<UUID, WebLine> = lines
		val invalidLines: MutableMap<RedstoneWebLine, WebLineInvalidation> = mutableMapOf()

		for (lineUuid in lineUuids) {
			val line = lines[lineUuid] ?: continue
			if (!line.isLoaded(level)) continue

			val invalidation = line.getInvalidation(level, commonLines) ?: continue
			if (invalidation.dependencyDepth != 0
				&& invalidation.reason != WebLineInvalidationReason.CYCLIC_DEPENDENCY
			) continue

			invalidLines[line] = invalidation
		}

		return invalidLines
	}

	private fun getLinesNearChangedChunks(): Set<UUID> {
		val lineUuids: MutableSet<UUID> = mutableSetOf()
		for (changedChunk in chunksToValidate) {
			for (chunkX in changedChunk.x - 1..changedChunk.x + 1) {
				for (chunkZ in changedChunk.z - 1..changedChunk.z + 1) {
					val cachedUuids = lineUuidsByChunk[ChunkPos(chunkX, chunkZ)] ?: continue
					lineUuids.addAll(cachedUuids)
				}
			}
		}

		chunksToValidate.clear()
		return lineUuids
	}

	private fun getLinesWithEndpointInChunk(chunkPos: ChunkPos): List<RedstoneWebLine> {
		val cachedUuids = lineUuidsByChunk[chunkPos] ?: return emptyList()
		val matchingLines: MutableList<RedstoneWebLine> = mutableListOf()

		for (lineUuid in cachedUuids) {
			val line = lines[lineUuid] ?: continue
			if (chunkPos !in line.getEndpointChunkPositions()) continue

			matchingLines.add(line)
		}

		return matchingLines
	}

	private fun removeStoredLine(level: ServerLevel, line: RedstoneWebLine) {
		lines.remove(line.uuid)
		removeLineReferences(line)
		updateBlockAnchors(level, line)
		sendToNearbyPlayers(level, line, RemoveWebLinePacket(WebLineType.REDSTONE, line.uuid))
		playBreakSound(level, line)
		scheduleDependentLines(level, line.uuid)

		val changedLines = refreshLinePower(level)
		synchronizePower(level, changedLines)
	}

	private fun isNearExistingNode(line: RedstoneWebLine, position: Vec3): Boolean {
		if (line.firstNode.position.distanceToSqr(position) <= NODE_OCCUPANCY_RADIUS_SQUARED) return true
		if (line.secondNode.position.distanceToSqr(position) <= NODE_OCCUPANCY_RADIUS_SQUARED) return true

		for (attachment in line.attachedAnchors) {
			if (attachment.anchor.position.distanceToSqr(position) <= NODE_OCCUPANCY_RADIUS_SQUARED) return true
		}

		return false
	}

	private fun getLinesAttachedTo(line: RedstoneWebLine): Set<RedstoneWebLine> {
		return getLinesAttachedTo(listOf(line))
	}

	private fun getLinesAttachedTo(parentLines: Collection<RedstoneWebLine>): Set<RedstoneWebLine> {
		val attachedLines: MutableSet<RedstoneWebLine> = mutableSetOf()

		for (parentLine in parentLines) {
			for (attachment in parentLine.attachedAnchors) {
				for (attachedLine in attachment.anchor.lines) {
					if (attachedLine is RedstoneWebLine && attachedLine !in parentLines) {
						attachedLines.add(attachedLine)
					}
				}
			}
		}

		return attachedLines
	}

	private fun synchronizeReplacementRemovals(
		level: ServerLevel,
		replacedLines: Collection<RedstoneWebLine>,
		affectedLines: Collection<RedstoneWebLine>
	) {
		for (line in replacedLines) {
			sendToNearbyPlayers(level, line, RemoveWebLinePacket(WebLineType.REDSTONE, line.uuid))
		}

		for (line in affectedLines) {
			sendToNearbyPlayers(level, line, RemoveWebLinePacket(WebLineType.REDSTONE, line.uuid))
		}
	}

	private fun removeLineForReplacement(level: ServerLevel, line: RedstoneWebLine) {
		lines.remove(line.uuid)
		removeLineReferences(line)
		updateBlockAnchors(level, line)
	}

	private fun addLineForReplacement(line: RedstoneWebLine) {
		nodes[line.firstNode.uuid] = line.firstNode
		nodes[line.secondNode.uuid] = line.secondNode
		lines[line.uuid] = line
		addLineReferences(line)
	}

	private fun finishLineReplacement(
		level: ServerLevel,
		newLines: Collection<RedstoneWebLine>,
		affectedLines: Collection<RedstoneWebLine>
	) {
		setDirty()

		val synchronizedLines: MutableSet<RedstoneWebLine> = affectedLines.toMutableSet()
		synchronizedLines.addAll(newLines)
		val addPacket = AddWebLinesPacket.fromLines(WebLineType.REDSTONE, synchronizedLines)
		sendToNearbyPlayers(level, synchronizedLines, addPacket)

		val changedLines = refreshLinePower(level).toMutableSet()
		changedLines.addAll(newLines)
		synchronizePower(level, changedLines)
	}

	private fun getOtherNode(line: RedstoneWebLine, node: WebNode): WebNode? {
		if (line.firstNode === node) return line.secondNode
		if (line.secondNode === node) return line.firstNode
		return null
	}

	private fun addLineReferences(line: RedstoneWebLine) {
		line.firstNode.addLine(line)
		line.secondNode.addLine(line)
		addAnchorToParentLine(line.firstNode)
		addAnchorToParentLine(line.secondNode)

		for (node in nodes.values) {
			if (node !is WebLineAnchor || node.parentLineUuid != line.uuid) continue
			line.addAttachedAnchor(node)
		}

		for (chunkPos in line.intersectedChunkPositions) {
			var lineUuids = lineUuidsByChunk[chunkPos]
			if (lineUuids == null) {
				lineUuids = mutableSetOf()
				lineUuidsByChunk[chunkPos] = lineUuids
			}

			lineUuids.add(line.uuid)
		}

		addSignalPositionReference(line, line.firstNode)
		addSignalPositionReference(line, line.secondNode)
	}

	private fun addLoadedLine(line: RedstoneWebLine) {
		nodes[line.firstNode.uuid] = line.firstNode
		nodes[line.secondNode.uuid] = line.secondNode
		lines[line.uuid] = line
		addLineReferences(line)
	}

	private fun addAnchorToParentLine(node: WebNode) {
		if (node !is WebLineAnchor) return

		val parentLine = lines[node.parentLineUuid] ?: return
		parentLine.addAttachedAnchor(node)
	}

	private fun addSignalPositionReference(line: RedstoneWebLine, node: WebNode) {
		if (node !is WebBlockAnchor) return

		val position = line.getSignalPosition(node)
		var lineUuids = lineUuidsBySignalPosition[position]
		if (lineUuids == null) {
			lineUuids = mutableSetOf()
			lineUuidsBySignalPosition[position] = lineUuids
		}

		lineUuids.add(line.uuid)
	}

	private fun removeLineReferences(line: RedstoneWebLine) {
		line.firstNode.removeLine(line)
		line.secondNode.removeLine(line)

		for (chunkPos in line.intersectedChunkPositions) {
			val lineUuids = lineUuidsByChunk[chunkPos] ?: continue
			lineUuids.remove(line.uuid)

			if (lineUuids.isEmpty()) {
				lineUuidsByChunk.remove(chunkPos)
			}
		}

		removeSignalPositionReference(line, line.firstNode)
		removeSignalPositionReference(line, line.secondNode)
		removeNodeIfOrphaned(line.firstNode)
		removeNodeIfOrphaned(line.secondNode)
		line.clearAttachedAnchors()
	}

	private fun removeSignalPositionReference(line: RedstoneWebLine, node: WebNode) {
		if (node !is WebBlockAnchor) return

		val position = line.getSignalPosition(node)
		val lineUuids = lineUuidsBySignalPosition[position] ?: return
		lineUuids.remove(line.uuid)

		if (lineUuids.isEmpty()) {
			lineUuidsBySignalPosition.remove(position)
		}
	}

	private fun removeNodeIfOrphaned(node: WebNode) {
		if (node.lines.isNotEmpty()) return

		if (node is WebLineAnchor) {
			lines[node.parentLineUuid]?.removeAttachedAnchor(node.uuid)
		}

		nodes.remove(node.uuid)
	}

	private fun getPoweredLines(line: RedstoneWebLine): Set<RedstoneWebLine> {
		val connectedLines: MutableSet<RedstoneWebLine> = mutableSetOf()
		addLinesPoweredThroughNode(line, line.firstNode, connectedLines)
		addLinesPoweredThroughNode(line, line.secondNode, connectedLines)

		for (attachment in line.attachedAnchors) {
			for (connectedLine in attachment.anchor.lines) {
				if (connectedLine is RedstoneWebLine) {
					connectedLines.add(connectedLine)
				}
			}
		}

		connectedLines.remove(line)
		return connectedLines
	}

	private fun addLinesPoweredThroughNode(
		sourceLine: RedstoneWebLine,
		node: WebNode,
		connectedLines: MutableSet<RedstoneWebLine>
	) {
		for (connectedLine in node.lines) {
			if (connectedLine !is RedstoneWebLine) continue
			if (node is Arachnode && !node.canPower(sourceLine, connectedLine)) continue

			connectedLines.add(connectedLine)
		}

		if (node !is WebLineAnchor) return

		val parentLine = lines[node.parentLineUuid] ?: return
		connectedLines.add(parentLine)
	}

	private fun scheduleDependentLines(level: ServerLevel, removedLineUuid: UUID) {
		val dependentLineUuids: MutableList<UUID> = mutableListOf()
		for (line in lines.values) {
			val firstParentLineUuid = (line.firstNode as? WebLineAnchor)?.parentLineUuid
			val secondParentLineUuid = (line.secondNode as? WebLineAnchor)?.parentLineUuid
			if (firstParentLineUuid != removedLineUuid && secondParentLineUuid != removedLineUuid) continue

			dependentLineUuids.add(line.uuid)
		}
		if (dependentLineUuids.isEmpty()) return

		level.scheduleTaskInTicks(CASCADE_DELAY_TICKS) {
			var removedLine = false
			val commonLines: Map<UUID, WebLine> = lines

			for (lineUuid in dependentLineUuids) {
				val line = lines[lineUuid] ?: continue
				if (!line.isLoaded(level)) continue
				if (line.getInvalidation(level, commonLines) == null) continue

				removeStoredLine(level, line)
				removedLine = true
			}

			if (removedLine) setDirty()
		}
	}

	private fun updateBlockAnchors(
		level: ServerLevel,
		lines: Collection<RedstoneWebLine>
	) {
		val updatedNodes: MutableSet<UUID> = mutableSetOf()
		for (line in lines) {
			updateBlockAnchor(level, line.firstNode, updatedNodes)
			updateBlockAnchor(level, line.secondNode, updatedNodes)
		}
	}

	private fun updateBlockAnchors(level: ServerLevel, line: RedstoneWebLine) {
		val updatedNodes: MutableSet<UUID> = mutableSetOf()
		updateBlockAnchor(level, line.firstNode, updatedNodes)
		updateBlockAnchor(level, line.secondNode, updatedNodes)
	}

	private fun updateBlockAnchor(
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

	private fun synchronizePower(level: ServerLevel, synchronizedLines: Collection<RedstoneWebLine>) {
		if (synchronizedLines.isEmpty()) return

		val powerData: MutableList<RedstoneWebPowerData> = mutableListOf()
		for (line in synchronizedLines) {
			powerData.add(RedstoneWebPowerData(line.uuid, line.power))
		}
		val packet = SyncRedstoneWebPowerPacket(powerData)
		val nearbyPlayers: MutableSet<ServerPlayer> = mutableSetOf()
		for (line in synchronizedLines) {
			for (chunkPos in line.getEndpointChunkPositions()) {
				nearbyPlayers.addAll(level.chunkSource.chunkMap.getPlayers(chunkPos, false))
			}
		}

		for (player in nearbyPlayers) {
			packet.messagePlayer(player)
		}
	}

	private fun synchronizePower(player: ServerPlayer, lines: Collection<RedstoneWebLine>) {
		val powerData: MutableList<RedstoneWebPowerData> = mutableListOf()
		for (line in lines) {
			powerData.add(RedstoneWebPowerData(line.uuid, line.power))
		}

		SyncRedstoneWebPowerPacket(powerData).messagePlayer(player)
	}

	private fun sendToNearbyPlayers(level: ServerLevel, line: WebLine, packet: AaronPacket) {
		val nearbyPlayers: MutableSet<ServerPlayer> = mutableSetOf()
		for (chunkPos in line.getEndpointChunkPositions()) {
			nearbyPlayers.addAll(level.chunkSource.chunkMap.getPlayers(chunkPos, false))
		}

		for (player in nearbyPlayers) {
			packet.messagePlayer(player)
		}
	}

	private fun sendToNearbyPlayers(
		level: ServerLevel,
		lines: Collection<WebLine>,
		packet: AaronPacket
	) {
		val nearbyPlayers: MutableSet<ServerPlayer> = mutableSetOf()
		for (line in lines) {
			for (chunkPos in line.getEndpointChunkPositions()) {
				nearbyPlayers.addAll(level.chunkSource.chunkMap.getPlayers(chunkPos, false))
			}
		}

		for (player in nearbyPlayers) {
			packet.messagePlayer(player)
		}
	}

	private fun playBreakSound(level: ServerLevel, line: WebLine) {
		val center = line.firstNode.position
			.add(line.secondNode.position)
			.scale(0.5)

		level.playSound(
			null,
			center.x,
			center.y,
			center.z,
			ModSoundEvents.WEB_SNAP.get(),
			SoundSource.BLOCKS,
			0.5f,
			2f
		)
	}

	@Synchronized
	override fun save(tag: CompoundTag, registries: HolderLookup.Provider): CompoundTag {
		val ops = registries.createSerializationContext(NbtOps.INSTANCE)

		val nodesTag = WebNode.CODEC
			.listOf()
			.encodeStart(ops, nodes.values.toList())
			.getOrThrow()
		val linesTag = WebLineData.CODEC
			.listOf()
			.encodeStart(ops, lines.values.map(WebLine::data))
			.getOrThrow()

		tag.put(NODES_TAG, nodesTag)
		tag.put(LINES_TAG, linesTag)
		return tag
	}

	companion object {
		private const val MAX_POWER = 15
		private const val NODE_OCCUPANCY_RADIUS_SQUARED = 0.09
		private const val CASCADE_DELAY_TICKS = 2
		private const val SAVED_DATA_NAME = "critterworks_redstone_webs"
		private const val NODES_TAG = "Nodes"
		private const val LINES_TAG = "Lines"
		private var suppressWebstonePower = false

		@JvmStatic
		fun isWebstonePowerSuppressed(): Boolean {
			return suppressWebstonePower
		}

		@JvmStatic
		fun getInjectedPower(level: ServerLevel, position: BlockPos, direction: Direction): Int {
			if (suppressWebstonePower) return 0
			return get(level).getPower(position, direction)
		}

		private fun load(tag: CompoundTag, registries: HolderLookup.Provider): RedstoneWebHandler {
			val handler = RedstoneWebHandler()

			val nodesTag = tag.get(NODES_TAG) ?: return handler
			val linesTag = tag.get(LINES_TAG) ?: return handler

			val ops = registries.createSerializationContext(NbtOps.INSTANCE)
			val loadedNodes = WebNode.CODEC
				.listOf()
				.parse(ops, nodesTag)
				.getOrThrow()
			val loadedLines = WebLineData.CODEC
				.listOf()
				.parse(ops, linesTag)
				.getOrThrow()

			val referencedNodeUuids: MutableSet<UUID> = mutableSetOf()
			for (lineData in loadedLines) {
				referencedNodeUuids.add(lineData.firstNodeUuid)
				referencedNodeUuids.add(lineData.secondNodeUuid)
			}

			for (node in loadedNodes) {
				if (node.uuid in referencedNodeUuids) {
					handler.nodes[node.uuid] = node
				}
			}

			for (lineData in loadedLines) {
				val firstNode = handler.nodes[lineData.firstNodeUuid] ?: continue
				val secondNode = handler.nodes[lineData.secondNodeUuid] ?: continue

				val line = RedstoneWebLine(
					lineData.uuid,
					firstNode,
					secondNode
				)

				handler.addLoadedLine(line)
			}

			return handler
		}

		fun get(level: ServerLevel): RedstoneWebHandler {
			return level.dataStorage.computeIfAbsent(
				Factory(::RedstoneWebHandler, ::load),
				SAVED_DATA_NAME
			)
		}
	}

}