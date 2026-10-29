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
import dev.aaronhowser.mods.critterworks.handler.web.redstone.part.RedstoneWebLine
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
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.saveddata.SavedData
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
		val linesBeingRead = lineUuidsBeingRead.get()
		var strongestPower = 0

		for (lineUuid in lineUuids) {
			val line = lines[lineUuid] ?: continue
			if (!line.connectsTo(position, direction)) continue

			if (linesBeingRead != null && lineUuid in linesBeingRead) continue

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
		val newPowerByLine: MutableMap<RedstoneWebLine, Int> = mutableMapOf()

		for (line in lines.values) {
			newPowerByLine[line] = calculateLinePower(level, line, mutableSetOf())
		}

		for ((line, newPower) in newPowerByLine) {
			if (!line.updatePower(newPower)) continue
			changedLines.add(line)
		}

		updateBlockAnchors(level, changedLines)

		return changedLines
	}

	private fun calculateLinePower(
		level: ServerLevel,
		line: RedstoneWebLine,
		checkedLines: MutableSet<UUID>
	): Int {
		if (!checkedLines.add(line.uuid)) return 0
		lineUuidsBeingRead.set(checkedLines)

		return try {
			var strongestPower = maxOf(
				getBlockInputPower(level, line.firstNode),
				getBlockInputPower(level, line.secondNode)
			)

			for (connectedLine in getConnectedLines(line)) {
				val connectedPower = calculateLinePower(level, connectedLine, checkedLines)
				strongestPower = maxOf(strongestPower, connectedPower)
				if (strongestPower >= MAX_POWER) break
			}

			strongestPower
		} finally {
			checkedLines.remove(line.uuid)
			if (checkedLines.isEmpty()) lineUuidsBeingRead.remove()
		}
	}

	private fun getBlockInputPower(
		level: ServerLevel,
		node: WebNode
	): Int {
		if (node !is WebBlockAnchor) return 0
		if (!node.isLoaded(level)) return 0

		return level.getSignal(node.blockPos, node.face.opposite)
			.coerceIn(0, MAX_POWER)
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

	private fun getConnectedLines(line: RedstoneWebLine): Set<RedstoneWebLine> {
		val connectedLines: MutableSet<RedstoneWebLine> = mutableSetOf()
		addLinesConnectedAtNode(line.firstNode, connectedLines)
		addLinesConnectedAtNode(line.secondNode, connectedLines)

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

	private fun addLinesConnectedAtNode(
		node: WebNode,
		connectedLines: MutableSet<RedstoneWebLine>
	) {
		for (connectedLine in node.lines) {
			if (connectedLine is RedstoneWebLine) {
				connectedLines.add(connectedLine)
			}
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
		private const val CASCADE_DELAY_TICKS = 2
		private const val SAVED_DATA_NAME = "critterworks_redstone_webs"
		private const val NODES_TAG = "Nodes"
		private const val LINES_TAG = "Lines"
		private val lineUuidsBeingRead = ThreadLocal<Set<UUID>>()

		@JvmStatic
		fun getInjectedPower(level: ServerLevel, position: BlockPos, direction: Direction): Int {
			return get(level).getPower(position, direction)
		}

		private fun load(tag: CompoundTag, registries: HolderLookup.Provider): RedstoneWebHandler {
			val savedData = RedstoneWebHandler()
			val ops = registries.createSerializationContext(NbtOps.INSTANCE)
			val nodesTag = tag.get(NODES_TAG)
			val linesTag = tag.get(LINES_TAG) ?: return savedData

			if (nodesTag == null) {
				val legacyLines = RedstoneWebLine.LEGACY_CODEC
					.listOf()
					.parse(ops, linesTag)
					.getOrThrow()

				for (line in legacyLines) {
					savedData.addLoadedLine(line)
				}

				return savedData
			}

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
				if (node.uuid in referencedNodeUuids) savedData.nodes[node.uuid] = node
			}

			for (lineData in loadedLines) {
				val firstNode = savedData.nodes[lineData.firstNodeUuid] ?: continue
				val secondNode = savedData.nodes[lineData.secondNodeUuid] ?: continue
				val line = RedstoneWebLine(
					lineData.uuid,
					firstNode,
					secondNode
				)
				savedData.addLoadedLine(line)
			}

			return savedData
		}

		private fun RedstoneWebHandler.addLoadedLine(line: RedstoneWebLine) {
			nodes[line.firstNode.uuid] = line.firstNode
			nodes[line.secondNode.uuid] = line.secondNode
			lines[line.uuid] = line
			addLineReferences(line)
		}

		fun get(level: ServerLevel): RedstoneWebHandler {
			return level.dataStorage.computeIfAbsent(FACTORY, SAVED_DATA_NAME)
		}

		private val FACTORY = Factory(::RedstoneWebHandler, ::load)
	}

}