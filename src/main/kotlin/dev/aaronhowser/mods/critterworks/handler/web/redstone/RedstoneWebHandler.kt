package dev.aaronhowser.mods.critterworks.handler.web.redstone

import dev.aaronhowser.mods.aaron.scheduler.SchedulerExtensions.scheduleTaskInTicks
import dev.aaronhowser.mods.critterworks.handler.web.WebLineType
import dev.aaronhowser.mods.critterworks.handler.web.common.line.WebLine
import dev.aaronhowser.mods.critterworks.handler.web.common.line.WebLineData
import dev.aaronhowser.mods.critterworks.handler.web.common.line.WebLineInvalidation
import dev.aaronhowser.mods.critterworks.handler.web.common.line.WebLineInvalidationReason
import dev.aaronhowser.mods.critterworks.handler.web.common.node.WebLineAnchor
import dev.aaronhowser.mods.critterworks.handler.web.common.node.WebNode
import dev.aaronhowser.mods.critterworks.handler.web.redstone.node.Arachnode
import dev.aaronhowser.mods.critterworks.handler.web.redstone.node.ArachnodeMount
import dev.aaronhowser.mods.critterworks.handler.web.redstone.node.ArachnodePort
import dev.aaronhowser.mods.critterworks.handler.web.redstone.power.RedstoneWebPowerController
import dev.aaronhowser.mods.critterworks.handler.web.redstone.power.RedstoneWebPowerSolver
import dev.aaronhowser.mods.critterworks.handler.web.redstone.storage.RedstoneWebGraph
import dev.aaronhowser.mods.critterworks.handler.web.redstone.sync.RedstoneWebSynchronizer
import dev.aaronhowser.mods.critterworks.item.arachnode.ArachnodeItem
import dev.aaronhowser.mods.critterworks.item.arachnode.ArachnodePlacementContext
import dev.aaronhowser.mods.critterworks.packet.server_to_client.AddWebLinesPacket
import dev.aaronhowser.mods.critterworks.packet.server_to_client.RemoveArachnodePacket
import dev.aaronhowser.mods.critterworks.packet.server_to_client.RemoveWebLinePacket
import dev.aaronhowser.mods.critterworks.registry.ModSoundEvents
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtOps
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundSource
import net.minecraft.world.Containers
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.saveddata.SavedData
import net.minecraft.world.phys.Vec3
import java.util.*

class RedstoneWebHandler : SavedData() {

	private val graph = RedstoneWebGraph()
	private val powerController = RedstoneWebPowerController(graph)

	private val nodes: Map<UUID, WebNode>
		get() = graph.nodes
	private val lines: Map<UUID, RedstoneWebLine>
		get() = graph.lines
	private val arachnodes: Map<UUID, Arachnode>
		get() = graph.arachnodes

	private val chunksToValidate: MutableSet<ChunkPos> = mutableSetOf()

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
			graph.removeLine(previousLine)
			powerController.notifyConnectedBlocks(level, previousLine)
		}

		graph.storeLine(storedLine)
		setDirty()

		val changedLines = powerController.updatePower(level)
		RedstoneWebSynchronizer.sendToTracking(
			level,
			storedLine,
			createAddLinesPacket(listOf(storedLine))
		)

		val linesToSynchronize: MutableSet<RedstoneWebLine> = changedLines.toMutableSet()
		linesToSynchronize.add(storedLine)
		RedstoneWebSynchronizer.synchronizePower(level, linesToSynchronize)
	}

	fun getNode(uuid: UUID): WebNode? {
		return graph.getNode(uuid)
	}

	fun getCanonicalNode(node: WebNode): WebNode {
		return graph.getCanonicalNode(node)
	}

	fun getLine(uuid: UUID): RedstoneWebLine? {
		return graph.getLine(uuid)
	}

	fun getArachnode(uuid: UUID): Arachnode? {
		return graph.getArachnode(uuid)
	}

	fun getOwningArachnode(port: ArachnodePort): Arachnode? {
		return graph.getOwningArachnode(port)
	}

	fun getPower(position: BlockPos, direction: Direction): Int {
		return graph.getPower(position, direction)
	}

	fun removeLine(level: ServerLevel, uuid: UUID): RedstoneWebLine? {
		val line = lines[uuid] ?: return null
		removeStoredLine(level, line)
		chunksToValidate.addAll(line.intersectedChunkPositions)
		setDirty()
		return line
	}

	fun insertArachnodeOnLine(
		level: ServerLevel,
		lineUuid: UUID,
		itemStack: ItemStack,
		position: Vec3,
		playerPosition: Vec3
	): Boolean {
		val line = lines[lineUuid] ?: return false
		if (isNearExistingNode(line, position)) return false

		val arachnodeItem = itemStack.item as? ArachnodeItem ?: return false
		val placementContext = ArachnodePlacementContext.OnLine(
			line,
			position,
			playerPosition,
			itemStack.copyWithCount(1)
		)
		val arachnode = arachnodeItem.createArachnode(placementContext) ?: return false
		val inputPort = arachnode.inputPorts.firstOrNull() ?: return false
		val outputPort = arachnode.outputPorts.firstOrNull() ?: return false
		val firstPort: ArachnodePort
		val secondPort: ArachnodePort
		if (inputPort.position.distanceToSqr(line.firstNode.position)
			<= outputPort.position.distanceToSqr(line.firstNode.position)
		) {
			firstPort = inputPort
			secondPort = outputPort
		} else {
			firstPort = outputPort
			secondPort = inputPort
		}
		val firstLineUuid = UUID.randomUUID()
		val secondLineUuid = UUID.randomUUID()

		val firstSegment = RedstoneWebLine(
			firstLineUuid,
			line.firstNode,
			firstPort,
			line.power
		)
		val secondSegment = RedstoneWebLine(
			secondLineUuid,
			secondPort,
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
		graph.storeArachnode(arachnode)
		graph.storeLine(firstSegment)
		graph.storeLine(secondSegment)
		finishLineReplacement(level, listOf(firstSegment, secondSegment), affectedLines)
		return true
	}

	fun placeSurfaceArachnode(
		level: ServerLevel,
		itemStack: ItemStack,
		blockPosition: BlockPos,
		face: Direction,
		clickedPosition: Vec3,
		playerPosition: Vec3
	): Boolean {
		val mount = ArachnodeMount.Surface(blockPosition, face, clickedPosition)
		if (!mount.isValid(level)) return false
		val position = clickedPosition.add(
			mount.getSurfaceNormal().scale(SURFACE_OFFSET)
		)
		for (existingArachnode in arachnodes.values) {
			if (existingArachnode.position.distanceToSqr(position)
				<= NODE_OCCUPANCY_RADIUS_SQUARED
			) return false
		}

		val arachnodeItem = itemStack.item as? ArachnodeItem ?: return false
		val placementContext = ArachnodePlacementContext.OnSurface(
			mount,
			position,
			playerPosition,
			itemStack.copyWithCount(1)
		)
		val arachnode = arachnodeItem.createArachnode(placementContext) ?: return false

		graph.storeArachnode(arachnode)
		setDirty()
		RedstoneWebSynchronizer.sendToTracking(
			level,
			arachnode,
			AddWebLinesPacket(
				WebLineType.REDSTONE,
				listOf(arachnode),
				emptyList(),
				emptyList()
			)
		)
		return true
	}

	fun removeArachnode(
		level: ServerLevel,
		arachnodeUuid: UUID
	): Arachnode? {
		val arachnode = arachnodes[arachnodeUuid] ?: return null
		val connectedLines: MutableList<RedstoneWebLine> = mutableListOf()
		for (port in arachnode.getPorts()) {
			for (line in port.lines) {
				if (line is RedstoneWebLine && line !in connectedLines) connectedLines.add(line)
			}
		}
		if (arachnode.mount is ArachnodeMount.Surface) {
			removeStoredLines(level, connectedLines, false)
			graph.removeArachnode(arachnode)
			synchronizeArachnodeRemoval(level, arachnode, connectedLines)
			setDirty()
			return arachnode
		}
		if (connectedLines.size != 2 || !hasConnectedInputAndOutput(arachnode)) {
			removeStoredLines(level, connectedLines, false)
			graph.removeArachnode(arachnode)
			synchronizeArachnodeRemoval(level, arachnode, connectedLines)
			setDirty()
			return arachnode
		}

		val firstLine = connectedLines[0]
		val secondLine = connectedLines[1]
		val firstNode = getOtherArachnodeEndpoint(firstLine, arachnode) ?: return null
		val secondNode = getOtherArachnodeEndpoint(secondLine, arachnode) ?: return null
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
		graph.removeArachnode(arachnode)
		synchronizeArachnodeRemoval(level, arachnode, replacedLines)
		graph.storeLine(fusedLine)
		finishLineReplacement(level, listOf(fusedLine), affectedLines)
		return arachnode
	}

	fun syncChunk(player: ServerPlayer, chunkPos: ChunkPos) {
		val nearbyLines = graph.getLinesIntersectingChunk(chunkPos)
		val nearbyArachnodes = graph.getArachnodesInChunk(chunkPos).toMutableSet()
		nearbyArachnodes.addAll(graph.getArachnodes(nearbyLines))
		if (nearbyLines.isEmpty() && nearbyArachnodes.isEmpty()) return

		AddWebLinesPacket
			.fromLines(WebLineType.REDSTONE, nearbyLines, nearbyArachnodes)
			.messagePlayer(player)
		RedstoneWebSynchronizer.synchronizePower(player, nearbyLines)
	}

	fun forgetChunk(player: ServerPlayer, chunkPos: ChunkPos) {
		val level = player.serverLevel()
		val arachnodesToCheck = graph.getArachnodesInChunk(chunkPos).toMutableSet()

		for (line in graph.getLinesIntersectingChunk(chunkPos)) {
			var stillTrackingLine = false

			for (intersectedChunk in line.intersectedChunkPositions) {
				if (intersectedChunk == chunkPos) continue

				val trackingPlayers = level.chunkSource.chunkMap.getPlayers(intersectedChunk, false)
				if (player !in trackingPlayers) continue

				stillTrackingLine = true
				break
			}

			if (!stillTrackingLine) {
				RemoveWebLinePacket(WebLineType.REDSTONE, line.uuid).messagePlayer(player)
				arachnodesToCheck.addAll(graph.getArachnodes(listOf(line)))
			}
		}

		for (arachnode in arachnodesToCheck) {
			if (isArachnodeTrackedOutsideChunk(level, player, arachnode, chunkPos)) continue
			RemoveArachnodePacket(arachnode.uuid).messagePlayer(player)
		}
	}

	private fun isArachnodeTrackedOutsideChunk(
		level: ServerLevel,
		player: ServerPlayer,
		arachnode: Arachnode,
		forgottenChunk: ChunkPos
	): Boolean {
		for (port in arachnode.getPorts()) {
			for (webLine in port.lines) {
				val line = webLine as? RedstoneWebLine ?: continue
				for (chunkPosition in line.intersectedChunkPositions) {
					if (chunkPosition == forgottenChunk) continue
					val trackingPlayers = level.chunkSource.chunkMap
						.getPlayers(chunkPosition, false)
					if (player in trackingPlayers) return true
				}
			}
		}

		return false
	}

	fun markForValidation(blockPos: BlockPos) {
		chunksToValidate.add(ChunkPos(blockPos))
	}

	fun tick(level: ServerLevel) {
		if (lines.isEmpty()) return

		val changedLines = powerController.updatePower(level)
		RedstoneWebSynchronizer.synchronizePower(level, changedLines)
	}

	fun validateChangedChunks(level: ServerLevel) {
		queuePeriodicValidation(level)
		if (chunksToValidate.isEmpty()) return

		val changedChunks = chunksToValidate.toSet()
		chunksToValidate.clear()
		val invalidLines = getInvalidLines(level, graph.getLinesNearChunks(changedChunks))

		removeStoredLines(level, invalidLines.keys)

		val invalidArachnodes: MutableList<Arachnode> = mutableListOf()
		for (arachnode in graph.getMountedArachnodesInSupportChunks(changedChunks)) {
			val mount = arachnode.mount as? ArachnodeMount.Surface ?: continue
			if (mount.isValid(level)) continue
			invalidArachnodes.add(arachnode)
		}

		for (arachnode in invalidArachnodes) {
			val removedArachnode = removeArachnode(level, arachnode.uuid) ?: continue
			Containers.dropItemStack(
				level,
				removedArachnode.position.x,
				removedArachnode.position.y,
				removedArachnode.position.z,
				removedArachnode.itemStack.copy()
			)
		}

		if (invalidLines.isNotEmpty() || invalidArachnodes.isNotEmpty()) setDirty()
	}

	private fun queuePeriodicValidation(level: ServerLevel) {
		if (level.gameTime % FULL_VALIDATION_INTERVAL_TICKS != 0L) return

		for (chunkPosition in graph.getLoadedValidationChunks()) {
			if (level.hasChunk(chunkPosition.x, chunkPosition.z)) {
				chunksToValidate.add(chunkPosition)
			}
		}
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

	private fun removeStoredLine(
		level: ServerLevel,
		line: RedstoneWebLine,
		removeOrphanedArachnodes: Boolean = true
	) {
		removeStoredLines(level, listOf(line), removeOrphanedArachnodes)
	}

	private fun removeStoredLines(
		level: ServerLevel,
		removedLines: Collection<RedstoneWebLine>,
		removeOrphanedArachnodes: Boolean = true
	) {
		if (removedLines.isEmpty()) return

		val attachedArachnodeUuids: MutableSet<UUID> = mutableSetOf()
		for (line in removedLines) {
			addArachnodeUuid(line.firstNode, attachedArachnodeUuids)
			addArachnodeUuid(line.secondNode, attachedArachnodeUuids)

			graph.removeLine(line)
			RedstoneWebSynchronizer.sendToTracking(
				level,
				line,
				RemoveWebLinePacket(WebLineType.REDSTONE, line.uuid)
			)
			playBreakSound(level, line)
			scheduleDependentLines(level, line.uuid)
		}

		powerController.notifyConnectedBlocks(level, removedLines)

		val changedLines = powerController.updatePower(level)
		RedstoneWebSynchronizer.synchronizePower(level, changedLines)

		if (removeOrphanedArachnodes) {
			removeOrphanedArachnodes(level, attachedArachnodeUuids)
		}
	}

	private fun hasConnectedInputAndOutput(arachnode: Arachnode): Boolean {
		var hasInput = false
		for (port in arachnode.inputPorts) {
			if (port.lines.isEmpty()) continue

			hasInput = true
			break
		}
		if (!hasInput) return false

		for (port in arachnode.outputPorts) {
			if (port.lines.isNotEmpty()) return true
		}

		return false
	}

	private fun addArachnodeUuid(node: WebNode, arachnodeUuids: MutableSet<UUID>) {
		if (node is ArachnodePort) arachnodeUuids.add(node.arachnodeUuid)
	}

	private fun removeOrphanedArachnodes(
		level: ServerLevel,
		arachnodeUuids: Collection<UUID>
	) {
		for (arachnodeUuid in arachnodeUuids) {
			val arachnode = arachnodes[arachnodeUuid] ?: continue
			if (hasConnectedLine(arachnode)) continue
			if (arachnode.mount is ArachnodeMount.Surface) continue

			graph.removeArachnode(arachnode)
			synchronizeArachnodeRemoval(level, arachnode)
			Containers.dropItemStack(
				level,
				arachnode.position.x,
				arachnode.position.y,
				arachnode.position.z,
				arachnode.itemStack.copy()
			)
			setDirty()
		}
	}

	private fun hasConnectedLine(arachnode: Arachnode): Boolean {
		for (port in arachnode.getPorts()) {
			if (port.lines.isNotEmpty()) return true
		}

		return false
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
			RedstoneWebSynchronizer.sendToTracking(
				level,
				line,
				RemoveWebLinePacket(WebLineType.REDSTONE, line.uuid)
			)
		}

		for (line in affectedLines) {
			RedstoneWebSynchronizer.sendToTracking(
				level,
				line,
				RemoveWebLinePacket(WebLineType.REDSTONE, line.uuid)
			)
		}
	}

	private fun removeLineForReplacement(level: ServerLevel, line: RedstoneWebLine) {
		graph.removeLine(line)
		powerController.notifyConnectedBlocks(level, line)
	}

	private fun finishLineReplacement(
		level: ServerLevel,
		newLines: Collection<RedstoneWebLine>,
		affectedLines: Collection<RedstoneWebLine>
	) {
		setDirty()

		val synchronizedLines: MutableSet<RedstoneWebLine> = affectedLines.toMutableSet()
		synchronizedLines.addAll(newLines)
		val addPacket = createAddLinesPacket(synchronizedLines)
		RedstoneWebSynchronizer.sendToTracking(level, synchronizedLines, addPacket)

		val changedLines = powerController.updatePower(level).toMutableSet()
		changedLines.addAll(newLines)
		RedstoneWebSynchronizer.synchronizePower(level, changedLines)
	}

	private fun getOtherArachnodeEndpoint(
		line: RedstoneWebLine,
		arachnode: Arachnode
	): WebNode? {
		if (line.firstNode is ArachnodePort
			&& line.firstNode.arachnodeUuid == arachnode.uuid
		) return line.secondNode
		if (line.secondNode is ArachnodePort
			&& line.secondNode.arachnodeUuid == arachnode.uuid
		) return line.firstNode
		return null
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

	private fun createAddLinesPacket(lines: Collection<RedstoneWebLine>): AddWebLinesPacket {
		return AddWebLinesPacket.fromLines(
			WebLineType.REDSTONE,
			lines,
			graph.getArachnodes(lines)
		)
	}

	private fun synchronizeArachnodeRemoval(
		level: ServerLevel,
		arachnode: Arachnode,
		connectedLines: Collection<WebLine> = emptyList()
	) {
		RedstoneWebSynchronizer.sendArachnodeRemoval(
			level,
			arachnode,
			connectedLines,
			RemoveArachnodePacket(arachnode.uuid)
		)
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

	override fun save(tag: CompoundTag, registries: HolderLookup.Provider): CompoundTag {
		val ops = registries.createSerializationContext(NbtOps.INSTANCE)

		val nodesTag = WebNode.CODEC
			.listOf()
			.encodeStart(ops, nodes.values.filter { node -> node !is ArachnodePort })
			.getOrThrow()
		val arachnodesTag = Arachnode.CODEC
			.listOf()
			.encodeStart(ops, arachnodes.values.toList())
			.getOrThrow()
		val linesTag = WebLineData.CODEC
			.listOf()
			.encodeStart(ops, lines.values.map(WebLine::data))
			.getOrThrow()

		tag.put(NODES_TAG, nodesTag)
		tag.put(ARACHNODES_TAG, arachnodesTag)
		tag.put(LINES_TAG, linesTag)
		return tag
	}

	companion object {
		private const val NODE_OCCUPANCY_RADIUS_SQUARED = 0.09
		private const val SURFACE_OFFSET = 0.001
		private const val CASCADE_DELAY_TICKS = 2
		private const val FULL_VALIDATION_INTERVAL_TICKS = 20
		private const val SAVED_DATA_NAME = "critterworks_redstone_webs"
		private const val NODES_TAG = "Nodes"
		private const val ARACHNODES_TAG = "Arachnodes"
		private const val LINES_TAG = "Lines"
		private val FACTORY = Factory(::RedstoneWebHandler, ::load)
		private val networksByLevel: MutableMap<ServerLevel, RedstoneWebHandler> = WeakHashMap()
		@JvmStatic
		fun isReadingRedstoneInputs(): Boolean {
			return RedstoneWebPowerSolver.isReadingInputs()
		}

		@JvmStatic
		fun getInjectedPower(level: ServerLevel, position: BlockPos, direction: Direction): Int {
			if (RedstoneWebPowerSolver.isReadingInputs()) return 0
			val network = networksByLevel[level] ?: return 0
			return network.getPower(position, direction)
		}

		private fun load(tag: CompoundTag, registries: HolderLookup.Provider): RedstoneWebHandler {
			val network = RedstoneWebHandler()

			val nodesTag = tag.get(NODES_TAG) ?: return network
			val arachnodesTag = tag.get(ARACHNODES_TAG) ?: return network
			val linesTag = tag.get(LINES_TAG) ?: return network

			val ops = registries.createSerializationContext(NbtOps.INSTANCE)
			val loadedNodes = WebNode.CODEC
				.listOf()
				.parse(ops, nodesTag)
				.getOrThrow()
			val loadedLines = WebLineData.CODEC
				.listOf()
				.parse(ops, linesTag)
				.getOrThrow()
			val loadedArachnodes = Arachnode.CODEC
				.listOf()
				.parse(ops, arachnodesTag)
				.getOrThrow()

			val referencedNodeUuids: MutableSet<UUID> = mutableSetOf()
			for (lineData in loadedLines) {
				referencedNodeUuids.add(lineData.firstNodeUuid)
				referencedNodeUuids.add(lineData.secondNodeUuid)
			}

			for (node in loadedNodes) {
				if (node.uuid in referencedNodeUuids) {
					network.graph.storeStandaloneNode(node)
				}
			}
			for (arachnode in loadedArachnodes) {
				network.graph.storeArachnode(arachnode)
			}

			for (lineData in loadedLines) {
				val firstNode = network.graph.getNode(lineData.firstNodeUuid) ?: continue
				val secondNode = network.graph.getNode(lineData.secondNodeUuid) ?: continue

				val line = RedstoneWebLine(
					lineData.uuid,
					firstNode,
					secondNode
				)

				network.graph.storeLine(line)
			}

			return network
		}

		fun get(level: ServerLevel): RedstoneWebHandler {
			val loadedNetwork = networksByLevel[level]
			if (loadedNetwork != null) return loadedNetwork

			val network = level.dataStorage.computeIfAbsent(FACTORY, SAVED_DATA_NAME)
			networksByLevel[level] = network
			return network
		}
	}

}