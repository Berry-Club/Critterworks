package dev.aaronhowser.mods.critterworks.handler.web

import com.mojang.serialization.DynamicOps
import dev.aaronhowser.mods.aaron.packet.AaronPacket
import dev.aaronhowser.mods.aaron.scheduler.SchedulerExtensions.scheduleTaskInTicks
import dev.aaronhowser.mods.critterworks.handler.web.line.WebLine
import dev.aaronhowser.mods.critterworks.handler.web.line.WebLineData
import dev.aaronhowser.mods.critterworks.handler.web.line.WebLineInvalidation
import dev.aaronhowser.mods.critterworks.handler.web.line.WebLineInvalidationReason
import dev.aaronhowser.mods.critterworks.handler.web.node.WebBlockAnchor
import dev.aaronhowser.mods.critterworks.handler.web.node.WebLineAnchor
import dev.aaronhowser.mods.critterworks.handler.web.node.WebNode
import dev.aaronhowser.mods.critterworks.packet.server_to_client.AddWebLinesPacket
import dev.aaronhowser.mods.critterworks.packet.server_to_client.RemoveWebLinePacket
import dev.aaronhowser.mods.critterworks.registry.ModSoundEvents
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtOps
import net.minecraft.nbt.Tag
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundSource
import net.minecraft.world.Containers
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.saveddata.SavedData
import java.util.UUID
import kotlin.collections.ArrayDeque

class WebSavedData : SavedData() {

	private val nodes: MutableMap<UUID, WebNode> = mutableMapOf()
	private val lines: MutableMap<UUID, WebLine> = mutableMapOf()
	private val networks: MutableSet<WebNetwork> = mutableSetOf()
	private val networksByLineUuid: MutableMap<UUID, WebNetwork> = mutableMapOf()

	private val lineUuidsByChunk: MutableMap<ChunkPos, MutableSet<UUID>> = mutableMapOf()
	private val chunksToValidate: MutableSet<ChunkPos> = mutableSetOf()

	@Synchronized
	fun addLine(level: ServerLevel, line: WebLine) {
		val previousLine = lines[line.uuid]
		if (previousLine != null) {
			removeLineFromNetwork(previousLine)
			removeLineReferences(level, previousLine)
		}

		nodes[line.firstNode.uuid] = line.firstNode
		nodes[line.secondNode.uuid] = line.secondNode

		lines[line.uuid] = line
		addLineReferences(line)
		addLineToNetwork(line)
		addToChunkCache(line)
		setDirty()
		sendToNearbyPlayers(level, line, AddWebLinesPacket.fromLines(listOf(line)))
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
	fun getLine(uuid: UUID): WebLine? {
		return lines[uuid]
	}

	@Synchronized
	fun getNetwork(lineUuid: UUID): WebNetwork? {
		return networksByLineUuid[lineUuid]
	}

	@Synchronized
	fun installWebPort(level: ServerLevel, anchor: WebBlockAnchor, webPortStack: ItemStack) {
		anchor.webPort = webPortStack.copyWithCount(1)
		syncAnchor(level, anchor)
	}

	@Synchronized
	fun removeWebPort(level: ServerLevel, anchor: WebBlockAnchor): ItemStack {
		val removedStack = anchor.webPort
		anchor.webPort = ItemStack.EMPTY
		syncAnchor(level, anchor)
		return removedStack
	}

	@Synchronized
	fun syncAnchor(level: ServerLevel, anchor: WebBlockAnchor) {
		setDirty()
		val packet = AddWebLinesPacket.fromLines(anchor.lines)
		for (line in anchor.lines) {
			sendToNearbyPlayers(level, line, packet)
		}
	}

	@Synchronized
	fun getNetworksAt(blockPos: BlockPos): Set<WebNetwork> {
		val matchingNetworks: MutableSet<WebNetwork> = mutableSetOf()
		for (node in nodes.values) {
			if (node !is WebBlockAnchor) continue
			if (node.blockPos != blockPos) continue

			for (line in node.lines) {
				val network = networksByLineUuid[line.uuid] ?: continue
				matchingNetworks.add(network)
			}
		}

		return matchingNetworks
	}

	@Synchronized
	fun removeLine(level: ServerLevel, uuid: UUID): WebLine? {
		val removedLine = lines[uuid] ?: return null
		removeStoredLine(level, removedLine)
		chunksToValidate.addAll(removedLine.intersectedChunkPositions)
		setDirty()
		return removedLine
	}

	@Synchronized
	fun syncChunk(player: ServerPlayer, chunkPos: ChunkPos) {
		val nearbyLines = lines.values.filter { line -> chunkPos in line.getEndpointChunkPositions() }
		if (nearbyLines.isEmpty()) return

		AddWebLinesPacket.fromLines(nearbyLines).messagePlayer(player)
	}

	@Synchronized
	fun forgetChunk(player: ServerPlayer, chunkPos: ChunkPos) {
		val level = player.serverLevel()

		for (line in lines.values) {
			val lineChunks = line.getEndpointChunkPositions()
			if (chunkPos !in lineChunks) continue

			var stillTrackingLine = false
			for (lineChunk in lineChunks) {
				if (lineChunk == chunkPos) continue

				val trackingPlayers = level.chunkSource.chunkMap.getPlayers(lineChunk, false)
				if (player !in trackingPlayers) continue

				stillTrackingLine = true
				break
			}

			if (!stillTrackingLine) {
				RemoveWebLinePacket(line.uuid).messagePlayer(player)
			}
		}
	}

	@Synchronized
	fun markForValidation(blockPos: BlockPos) {
		chunksToValidate.add(ChunkPos(blockPos))
	}

	@Synchronized
	fun validateChangedChunks(level: ServerLevel) {
		if (chunksToValidate.isEmpty()) return

		val lineUuids = getLinesNearChangedChunks()
		val invalidLines = getInvalidLines(level, lineUuids)
		if (invalidLines.isEmpty()) return

		removeInvalidLines(level, invalidLines)
		setDirty()
	}

	private fun getLinesNearChangedChunks(): Set<UUID> {
		val lineUuids: MutableSet<UUID> = mutableSetOf()
		for (changedChunk in chunksToValidate) {
			for (chunkX in changedChunk.x - 1..changedChunk.x + 1) {
				for (chunkZ in changedChunk.z - 1..changedChunk.z + 1) {
					val chunkPos = ChunkPos(chunkX, chunkZ)
					val cachedUuids = lineUuidsByChunk[chunkPos] ?: continue
					lineUuids.addAll(cachedUuids)
				}
			}
		}

		chunksToValidate.clear()
		return lineUuids
	}

	private fun getInvalidLines(
		level: ServerLevel,
		lineUuids: Set<UUID>
	): Map<WebLine, WebLineInvalidation> {
		val invalidLines: MutableMap<WebLine, WebLineInvalidation> = mutableMapOf()
		for (lineUuid in lineUuids) {
			val line = lines[lineUuid] ?: continue
			if (!line.isLoaded(level)) continue

			val invalidation = line.getInvalidation(level, lines) ?: continue
			if (invalidation.dependencyDepth != 0
				&& invalidation.reason != WebLineInvalidationReason.CYCLIC_DEPENDENCY
			) continue
			invalidLines[line] = invalidation
		}

		return invalidLines
	}

	private fun removeInvalidLines(
		level: ServerLevel,
		invalidLines: Map<WebLine, WebLineInvalidation>
	) {
		for (line in invalidLines.keys) {
			removeStoredLine(level, line)
		}
	}

	private fun removeStoredLine(level: ServerLevel, line: WebLine) {
		lines.remove(line.uuid)
		removeLineFromNetwork(line)
		removeLineReferences(level, line)
		sendToNearbyPlayers(level, line, RemoveWebLinePacket(line.uuid))
		playBreakSound(level, line)
		scheduleDependentLines(level, line.uuid)
	}

	private fun removeLineReferences(level: ServerLevel, line: WebLine) {
		removeFromChunkCache(line)
		line.firstNode.removeLine(line)
		line.secondNode.removeLine(line)
		removeNodeIfOrphaned(line.firstNode.uuid, level)
		removeNodeIfOrphaned(line.secondNode.uuid, level)
		line.clearAttachedAnchors()
	}

	private fun addLineReferences(line: WebLine) {
		line.firstNode.addLine(line)
		line.secondNode.addLine(line)
		addAnchorToReferencedLine(line.firstNode)
		addAnchorToReferencedLine(line.secondNode)

		for (node in nodes.values) {
			if (node !is WebLineAnchor || node.lineUuid != line.uuid) continue
			line.addAttachedAnchor(node)
		}
	}

	private fun addAnchorToReferencedLine(node: WebNode) {
		if (node !is WebLineAnchor) return

		val referencedLine = lines[node.lineUuid] ?: return
		referencedLine.addAttachedAnchor(node)
	}

	private fun addLineToNetwork(line: WebLine) {
		val connectedNetworks: MutableSet<WebNetwork> = mutableSetOf()
		for (otherLine in lines.values) {
			if (otherLine.uuid == line.uuid) continue
			if (!areConnected(line, otherLine)) continue

			val network = networksByLineUuid[otherLine.uuid] ?: continue
			connectedNetworks.add(network)
		}

		val network = connectedNetworks.firstOrNull() ?: WebNetwork()
		if (connectedNetworks.isEmpty()) {
			networks.add(network)
		} else {
			for (connectedNetwork in connectedNetworks) {
				if (connectedNetwork === network) continue

				for (connectedLine in connectedNetwork.lines) {
					network.addLine(connectedLine)
					networksByLineUuid[connectedLine.uuid] = network
				}

				connectedNetwork.clear()
				networks.remove(connectedNetwork)
			}
		}

		network.addLine(line)
		networksByLineUuid[line.uuid] = network
	}

	private fun removeLineFromNetwork(line: WebLine) {
		val network = networksByLineUuid.remove(line.uuid) ?: return
		network.removeLine(line)

		if (network.lines.isEmpty()) {
			networks.remove(network)
			return
		}

		rebuildNetworkComponents(network)
	}

	private fun rebuildNetworkComponents(network: WebNetwork) {
		val remainingLines = network.lines.toMutableSet()
		val components: MutableList<Set<WebLine>> = mutableListOf()

		while (remainingLines.isNotEmpty()) {
			val component: MutableSet<WebLine> = mutableSetOf()
			val pendingLines: ArrayDeque<WebLine> = ArrayDeque()
			pendingLines.addLast(remainingLines.first())

			while (pendingLines.isNotEmpty()) {
				val currentLine = pendingLines.removeFirst()
				if (!remainingLines.remove(currentLine)) continue

				component.add(currentLine)
				for (candidate in remainingLines) {
					if (areConnected(currentLine, candidate)) {
						pendingLines.addLast(candidate)
					}
				}
			}

			components.add(component)
		}

		network.clear()
		assignComponent(network, components.first())

		for (componentIndex in 1 until components.size) {
			val splitNetwork = WebNetwork()
			networks.add(splitNetwork)
			assignComponent(splitNetwork, components[componentIndex])
		}
	}

	private fun assignComponent(network: WebNetwork, component: Collection<WebLine>) {
		network.addLines(component)
		for (line in component) {
			networksByLineUuid[line.uuid] = network
		}
	}

	private fun areConnected(firstLine: WebLine, secondLine: WebLine): Boolean {
		if (firstLine.firstNode.uuid == secondLine.firstNode.uuid) return true
		if (firstLine.firstNode.uuid == secondLine.secondNode.uuid) return true
		if (firstLine.secondNode.uuid == secondLine.firstNode.uuid) return true
		if (firstLine.secondNode.uuid == secondLine.secondNode.uuid) return true

		return referencesLine(firstLine, secondLine.uuid)
			|| referencesLine(secondLine, firstLine.uuid)
	}

	private fun referencesLine(line: WebLine, referencedLineUuid: UUID): Boolean {
		val firstLineUuid = (line.firstNode as? WebLineAnchor)?.lineUuid
		if (firstLineUuid == referencedLineUuid) return true

		val secondLineUuid = (line.secondNode as? WebLineAnchor)?.lineUuid
		return secondLineUuid == referencedLineUuid
	}

	private fun scheduleDependentLines(level: ServerLevel, removedLineUuid: UUID) {
		val dependentLineUuids: MutableList<UUID> = mutableListOf()
		for (line in lines.values) {
			val firstParentUuid = (line.firstNode as? WebLineAnchor)?.lineUuid
			val secondParentUuid = (line.secondNode as? WebLineAnchor)?.lineUuid
			if (firstParentUuid != removedLineUuid && secondParentUuid != removedLineUuid) continue

			dependentLineUuids.add(line.uuid)
		}
		if (dependentLineUuids.isEmpty()) return

		level.scheduleTaskInTicks(CASCADE_DELAY_TICKS) {
			var removedLine = false
			for (lineUuid in dependentLineUuids) {
				val line = lines[lineUuid] ?: continue
				if (!line.isLoaded(level)) continue
				if (line.getInvalidation(level, lines) == null) continue

				removeStoredLine(level, line)
				removedLine = true
			}

			if (removedLine) setDirty()
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

	private fun addToChunkCache(line: WebLine) {
		for (chunkPos in line.intersectedChunkPositions) {
			var lineUuids = lineUuidsByChunk[chunkPos]
			if (lineUuids == null) {
				lineUuids = mutableSetOf()
				lineUuidsByChunk[chunkPos] = lineUuids
			}

			lineUuids.add(line.uuid)
		}
	}

	private fun removeFromChunkCache(line: WebLine) {
		for (chunkPos in line.intersectedChunkPositions) {
			val lineUuids = lineUuidsByChunk[chunkPos] ?: continue
			lineUuids.remove(line.uuid)

			if (lineUuids.isEmpty()) {
				lineUuidsByChunk.remove(chunkPos)
			}
		}
	}

	private fun removeNodeIfOrphaned(nodeUuid: UUID, level: ServerLevel) {
		val node = nodes[nodeUuid] ?: return
		if (node.lines.isNotEmpty()) return

		if (node is WebLineAnchor) {
			lines[node.lineUuid]?.removeAttachedAnchor(node.uuid)
		}
		if (node is WebBlockAnchor && node.hasWebPort) {
			Containers.dropItemStack(
				level,
				node.position.x,
				node.position.y,
				node.position.z,
				node.webPort
			)
		}

		nodes.remove(nodeUuid)
	}

	private fun sendToNearbyPlayers(level: ServerLevel, line: WebLine, packet: AaronPacket) {
		val nearbyPlayers: MutableSet<ServerPlayer> = mutableSetOf()

		for (chunkPos in line.getEndpointChunkPositions()) {
			for (player in level.chunkSource.chunkMap.getPlayers(chunkPos, false)) {
				nearbyPlayers.add(player)
			}
		}

		for (player in nearbyPlayers) {
			packet.messagePlayer(player)
		}
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
		private const val CASCADE_DELAY_TICKS = 2
		private const val SAVED_DATA_NAME = "critterworks_webs"
		private const val NODES_TAG = "Nodes"
		private const val LINES_TAG = "Lines"

		private fun load(tag: CompoundTag, registries: HolderLookup.Provider): WebSavedData {
			val savedData = WebSavedData()
			val nodesTag = tag.get(NODES_TAG) ?: return savedData
			val linesTag = tag.get(LINES_TAG) ?: return savedData
			val ops: DynamicOps<Tag> = registries.createSerializationContext(NbtOps.INSTANCE)
			val loadedNodes = WebNode.CODEC
				.listOf()
				.parse(ops, nodesTag)
				.getOrThrow()
			val loadedLines = WebLineData.CODEC
				.listOf()
				.parse(ops, linesTag)
				.getOrThrow()

			for (node in loadedNodes) {
				savedData.nodes[node.uuid] = node
			}

			for (lineData in loadedLines) {
				val firstNode = savedData.nodes[lineData.firstNodeUuid] ?: continue
				val secondNode = savedData.nodes[lineData.secondNodeUuid] ?: continue
				val line = WebLine(lineData.uuid, firstNode, secondNode)
				savedData.lines[line.uuid] = line
				savedData.addLineReferences(line)
				savedData.addLineToNetwork(line)
				savedData.addToChunkCache(line)
			}

			return savedData
		}

		fun get(level: ServerLevel): WebSavedData {
			if (level != level.server.overworld()) {
				return get(level.server.overworld())
			}

			return level.dataStorage.computeIfAbsent(FACTORY, SAVED_DATA_NAME)
		}

		private val FACTORY = Factory(::WebSavedData, ::load)
	}
}