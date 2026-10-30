package dev.aaronhowser.mods.critterworks.handler.web.redstone

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
import dev.aaronhowser.mods.critterworks.handler.web.redstone.node.ArachnodeMount
import dev.aaronhowser.mods.critterworks.handler.web.redstone.node.ArachnodePort
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
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.saveddata.SavedData
import net.minecraft.world.phys.Vec3
import java.util.*

class RedstoneWebHandler : SavedData() {

	private val nodes: MutableMap<UUID, WebNode> = mutableMapOf()
	private val lines: MutableMap<UUID, RedstoneWebLine> = mutableMapOf()
	private val arachnodes: MutableMap<UUID, Arachnode> = mutableMapOf()

	private val lineUuidsByChunk: MutableMap<ChunkPos, MutableSet<UUID>> = mutableMapOf()
	private val lineUuidsBySignalPosition: MutableMap<BlockPos, MutableSet<UUID>> = mutableMapOf()
	private val arachnodeUuidsByChunk: MutableMap<ChunkPos, MutableSet<UUID>> = mutableMapOf()
	private val mountedArachnodeUuidsBySupportChunk: MutableMap<ChunkPos, MutableSet<UUID>> = mutableMapOf()
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
		RedstoneWebSynchronizer.sendToTracking(
			level,
			storedLine,
			createAddLinesPacket(listOf(storedLine))
		)

		val linesToSynchronize: MutableSet<RedstoneWebLine> = changedLines.toMutableSet()
		linesToSynchronize.add(storedLine)
		RedstoneWebSynchronizer.synchronizePower(level, linesToSynchronize)
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
	fun getArachnode(uuid: UUID): Arachnode? {
		return arachnodes[uuid]
	}

	@Synchronized
	fun getArachnode(port: ArachnodePort): Arachnode? {
		return arachnodes[port.arachnodeUuid]
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
		addArachnodeReferences(arachnode)
		addLineForReplacement(firstSegment)
		addLineForReplacement(secondSegment)
		finishLineReplacement(level, listOf(firstSegment, secondSegment), affectedLines)
		return true
	}

	@Synchronized
	fun addMountedArachnode(
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

		addArachnodeReferences(arachnode)
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

	@Synchronized
	fun removeArachnode(
		level: ServerLevel,
		nodeUuid: UUID
	): Arachnode? {
		val arachnode = arachnodes[nodeUuid] ?: return null
		val connectedLines: MutableList<RedstoneWebLine> = mutableListOf()
		for (port in arachnode.getPorts()) {
			for (line in port.lines) {
				if (line is RedstoneWebLine && line !in connectedLines) connectedLines.add(line)
			}
		}
		if (arachnode.mount is ArachnodeMount.Surface) {
			removeStoredLines(level, connectedLines, false)
			removeArachnodeReferences(arachnode)
			synchronizeArachnodeRemoval(level, arachnode)
			setDirty()
			return arachnode
		}
		if (connectedLines.size > 2) return null
		if (connectedLines.size != 2 || !hasConnectedInputAndOutput(arachnode)) {
			removeStoredLines(level, connectedLines, false)
			removeArachnodeReferences(arachnode)
			synchronizeArachnodeRemoval(level, arachnode)
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
		removeArachnodeReferences(arachnode)
		synchronizeArachnodeRemoval(level, arachnode)
		addLineForReplacement(fusedLine)
		finishLineReplacement(level, listOf(fusedLine), affectedLines)
		return arachnode
	}

	@Synchronized
	fun syncChunk(player: ServerPlayer, chunkPos: ChunkPos) {
		val nearbyLines = getLinesIntersectingChunk(chunkPos)
		val nearbyArachnodes = getArachnodesInChunk(chunkPos).toMutableSet()
		nearbyArachnodes.addAll(getArachnodes(nearbyLines))
		if (nearbyLines.isEmpty() && nearbyArachnodes.isEmpty()) return

		AddWebLinesPacket
			.fromLines(WebLineType.REDSTONE, nearbyLines, nearbyArachnodes)
			.messagePlayer(player)
		RedstoneWebSynchronizer.synchronizePower(player, nearbyLines)
	}

	@Synchronized
	fun forgetChunk(player: ServerPlayer, chunkPos: ChunkPos) {
		val level = player.serverLevel()

		for (line in getLinesIntersectingChunk(chunkPos)) {
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
			}
		}

		for (arachnode in getArachnodesInChunk(chunkPos)) {
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

	@Synchronized
	fun markForValidation(blockPos: BlockPos) {
		chunksToValidate.add(ChunkPos(blockPos))
	}

	@Synchronized
	fun tick(level: ServerLevel) {
		val changedLines = refreshLinePower(level)
		RedstoneWebSynchronizer.synchronizePower(level, changedLines)
	}

	private fun refreshLinePower(level: ServerLevel): Set<RedstoneWebLine> {
		val changedLines: MutableSet<RedstoneWebLine> = mutableSetOf()
		val solver = RedstonePowerSolver(lines, arachnodes.values)
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

			updateBlockAnchors(level, lines.values)
			desiredPower = solver.solve(level)
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

	@Synchronized
	fun validateChangedChunks(level: ServerLevel) {
		if (chunksToValidate.isEmpty()) return

		val changedChunks = chunksToValidate.toSet()
		chunksToValidate.clear()
		val invalidLines = getInvalidLines(level, getLinesNearChangedChunks(changedChunks))

		removeStoredLines(level, invalidLines.keys)

		val invalidArachnodes: MutableList<Arachnode> = mutableListOf()
		for (arachnode in getMountedArachnodesInSupportChunks(changedChunks)) {
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

	private fun getLinesNearChangedChunks(changedChunks: Collection<ChunkPos>): Set<UUID> {
		val lineUuids: MutableSet<UUID> = mutableSetOf()
		for (changedChunk in changedChunks) {
			for (chunkX in changedChunk.x - 1..changedChunk.x + 1) {
				for (chunkZ in changedChunk.z - 1..changedChunk.z + 1) {
					val cachedUuids = lineUuidsByChunk[ChunkPos(chunkX, chunkZ)] ?: continue
					lineUuids.addAll(cachedUuids)
				}
			}
		}

		return lineUuids
	}

	private fun getLinesIntersectingChunk(chunkPos: ChunkPos): List<RedstoneWebLine> {
		val cachedUuids = lineUuidsByChunk[chunkPos] ?: return emptyList()
		val matchingLines: MutableList<RedstoneWebLine> = mutableListOf()

		for (lineUuid in cachedUuids) {
			val line = lines[lineUuid] ?: continue
			matchingLines.add(line)
		}

		return matchingLines
	}

	private fun getArachnodesInChunk(chunkPos: ChunkPos): List<Arachnode> {
		val arachnodeUuids = arachnodeUuidsByChunk[chunkPos] ?: return emptyList()
		val matchingArachnodes: MutableList<Arachnode> = mutableListOf()
		for (arachnodeUuid in arachnodeUuids) {
			val arachnode = arachnodes[arachnodeUuid] ?: continue
			matchingArachnodes.add(arachnode)
		}

		return matchingArachnodes
	}

	private fun getMountedArachnodesInSupportChunks(
		chunkPositions: Collection<ChunkPos>
	): Set<Arachnode> {
		val matchingArachnodes: MutableSet<Arachnode> = mutableSetOf()
		for (chunkPosition in chunkPositions) {
			val arachnodeUuids = mountedArachnodeUuidsBySupportChunk[chunkPosition] ?: continue
			for (arachnodeUuid in arachnodeUuids) {
				val arachnode = arachnodes[arachnodeUuid] ?: continue
				matchingArachnodes.add(arachnode)
			}
		}

		return matchingArachnodes
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

			lines.remove(line.uuid)
			removeLineReferences(line)
			RedstoneWebSynchronizer.sendToTracking(
				level,
				line,
				RemoveWebLinePacket(WebLineType.REDSTONE, line.uuid)
			)
			playBreakSound(level, line)
			scheduleDependentLines(level, line.uuid)
		}

		updateBlockAnchors(level, removedLines)

		val changedLines = refreshLinePower(level)
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

			removeArachnodeReferences(arachnode)
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
		val addPacket = createAddLinesPacket(synchronizedLines)
		RedstoneWebSynchronizer.sendToTracking(level, synchronizedLines, addPacket)

		val changedLines = refreshLinePower(level).toMutableSet()
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

	private fun addArachnodeReferences(arachnode: Arachnode) {
		arachnodes[arachnode.uuid] = arachnode
		for (port in arachnode.getPorts()) nodes[port.uuid] = port

		addArachnodeIndexReference(
			arachnodeUuidsByChunk,
			ChunkPos(BlockPos.containing(arachnode.position)),
			arachnode.uuid
		)
		val mount = arachnode.mount
		if (mount is ArachnodeMount.Surface) {
			addArachnodeIndexReference(
				mountedArachnodeUuidsBySupportChunk,
				ChunkPos(mount.blockPosition),
				arachnode.uuid
			)
		}
	}

	private fun removeArachnodeReferences(arachnode: Arachnode) {
		arachnodes.remove(arachnode.uuid)
		for (port in arachnode.getPorts()) nodes.remove(port.uuid)

		removeArachnodeIndexReference(
			arachnodeUuidsByChunk,
			ChunkPos(BlockPos.containing(arachnode.position)),
			arachnode.uuid
		)
		val mount = arachnode.mount
		if (mount is ArachnodeMount.Surface) {
			removeArachnodeIndexReference(
				mountedArachnodeUuidsBySupportChunk,
				ChunkPos(mount.blockPosition),
				arachnode.uuid
			)
		}
	}

	private fun addArachnodeIndexReference(
		index: MutableMap<ChunkPos, MutableSet<UUID>>,
		chunkPosition: ChunkPos,
		arachnodeUuid: UUID
	) {
		var arachnodeUuids = index[chunkPosition]
		if (arachnodeUuids == null) {
			arachnodeUuids = mutableSetOf()
			index[chunkPosition] = arachnodeUuids
		}

		arachnodeUuids.add(arachnodeUuid)
	}

	private fun removeArachnodeIndexReference(
		index: MutableMap<ChunkPos, MutableSet<UUID>>,
		chunkPosition: ChunkPos,
		arachnodeUuid: UUID
	) {
		val arachnodeUuids = index[chunkPosition] ?: return
		arachnodeUuids.remove(arachnodeUuid)
		if (arachnodeUuids.isEmpty()) index.remove(chunkPosition)
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
		if (node is ArachnodePort && node.arachnodeUuid in arachnodes) return

		if (node is WebLineAnchor) {
			lines[node.parentLineUuid]?.removeAttachedAnchor(node.uuid)
		}

		nodes.remove(node.uuid)
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

	private fun createAddLinesPacket(lines: Collection<RedstoneWebLine>): AddWebLinesPacket {
		return AddWebLinesPacket.fromLines(
			WebLineType.REDSTONE,
			lines,
			getArachnodes(lines)
		)
	}

	private fun getArachnodes(lines: Collection<RedstoneWebLine>): Set<Arachnode> {
		val matchingArachnodes: MutableSet<Arachnode> = mutableSetOf()
		for (line in lines) {
			addArachnode(line.firstNode, matchingArachnodes)
			addArachnode(line.secondNode, matchingArachnodes)
		}

		return matchingArachnodes
	}

	private fun addArachnode(node: WebNode, matchingArachnodes: MutableSet<Arachnode>) {
		if (node !is ArachnodePort) return

		val arachnode = arachnodes[node.arachnodeUuid] ?: return
		matchingArachnodes.add(arachnode)
	}

	private fun synchronizeArachnodeRemoval(level: ServerLevel, arachnode: Arachnode) {
		RedstoneWebSynchronizer.sendToTracking(
			level,
			arachnode,
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

	@Synchronized
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
		private const val MAX_POWER = 15
		private const val NODE_OCCUPANCY_RADIUS_SQUARED = 0.09
		private const val SURFACE_OFFSET = 0.001
		private const val CASCADE_DELAY_TICKS = 2
		private const val SAVED_DATA_NAME = "critterworks_redstone_webs"
		private const val NODES_TAG = "Nodes"
		private const val ARACHNODES_TAG = "Arachnodes"
		private const val LINES_TAG = "Lines"
		@JvmStatic
		fun isWebstonePowerSuppressed(): Boolean {
			return RedstoneWebPowerSuppression.isSuppressed()
		}

		@JvmStatic
		fun getInjectedPower(level: ServerLevel, position: BlockPos, direction: Direction): Int {
			if (RedstoneWebPowerSuppression.isSuppressed()) return 0
			return get(level).getPower(position, direction)
		}

		private fun load(tag: CompoundTag, registries: HolderLookup.Provider): RedstoneWebHandler {
			val handler = RedstoneWebHandler()

			val nodesTag = tag.get(NODES_TAG) ?: return handler
			val arachnodesTag = tag.get(ARACHNODES_TAG) ?: return handler
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
					handler.nodes[node.uuid] = node
				}
			}
			for (arachnode in loadedArachnodes) {
				handler.addArachnodeReferences(arachnode)
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