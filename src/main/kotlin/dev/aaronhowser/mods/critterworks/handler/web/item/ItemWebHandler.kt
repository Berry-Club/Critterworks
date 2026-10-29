package dev.aaronhowser.mods.critterworks.handler.web.item

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
import dev.aaronhowser.mods.critterworks.packet.server_to_client.AddWebLinesPacket
import dev.aaronhowser.mods.critterworks.packet.server_to_client.RemoveWebLinePacket
import dev.aaronhowser.mods.critterworks.packet.server_to_client.SyncItemWebPortsPacket
import dev.aaronhowser.mods.critterworks.registry.ModSoundEvents
import net.minecraft.core.BlockPos
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
import java.util.UUID
import kotlin.collections.ArrayDeque

class ItemWebHandler : SavedData() {

	private val nodes: MutableMap<UUID, WebNode> = mutableMapOf()
	private val lines: MutableMap<UUID, WebLine> = mutableMapOf()
	private val networks: MutableSet<WebNetwork> = mutableSetOf()
	private val networksByLineUuid: MutableMap<UUID, WebNetwork> = mutableMapOf()
	private val webPortsByNodeUuid: MutableMap<UUID, ItemStack> = mutableMapOf()

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
		sendToNearbyPlayers(
			level,
			line,
			AddWebLinesPacket.fromLines(WebLineType.ITEM, listOf(line))
		)
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
	fun getNetwork(node: WebNode): WebNetwork? {
		for (line in node.lines) {
			val network = networksByLineUuid[line.uuid]
			if (network != null) return network
		}

		return null
	}

	@Synchronized
	fun installWebPort(level: ServerLevel, anchor: WebBlockAnchor, webPortStack: ItemStack) {
		webPortsByNodeUuid[anchor.uuid] = webPortStack.copyWithCount(1)
		syncAnchor(level, anchor)
	}

	@Synchronized
	fun removeWebPort(level: ServerLevel, anchor: WebBlockAnchor): ItemStack {
		val removedStack = webPortsByNodeUuid.remove(anchor.uuid) ?: ItemStack.EMPTY
		syncAnchor(level, anchor)
		return removedStack
	}

	@Synchronized
	fun getWebPort(anchor: WebBlockAnchor): ItemStack {
		return webPortsByNodeUuid[anchor.uuid] ?: ItemStack.EMPTY
	}

	@Synchronized
	fun hasWebPort(anchor: WebBlockAnchor): Boolean {
		return anchor.uuid in webPortsByNodeUuid
	}

	@Synchronized
	fun syncAnchor(level: ServerLevel, anchor: WebBlockAnchor) {
		setDirty()
		val packet = SyncItemWebPortsPacket(
			listOf(ItemWebPortData(anchor.uuid, getWebPort(anchor)))
		)
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
		val nearbyLines = getLinesWithEndpointInChunk(chunkPos)
		if (nearbyLines.isEmpty()) return

		AddWebLinesPacket
			.fromLines(WebLineType.ITEM, nearbyLines)
			.messagePlayer(player)
		syncWebPorts(player, nearbyLines)
	}

	@Synchronized
	fun forgetChunk(player: ServerPlayer, chunkPos: ChunkPos) {
		val level = player.serverLevel()

		for (line in getLinesWithEndpointInChunk(chunkPos)) {
			val lineChunks = line.getEndpointChunkPositions()

			var stillTrackingLine = false
			for (lineChunk in lineChunks) {
				if (lineChunk == chunkPos) continue

				val trackingPlayers = level.chunkSource.chunkMap.getPlayers(lineChunk, false)
				if (player !in trackingPlayers) continue

				stillTrackingLine = true
				break
			}

			if (!stillTrackingLine) {
				RemoveWebLinePacket(WebLineType.ITEM, line.uuid).messagePlayer(player)
			}
		}
	}

	private fun getLinesWithEndpointInChunk(chunkPos: ChunkPos): List<WebLine> {
		val cachedUuids = lineUuidsByChunk[chunkPos] ?: return emptyList()
		val matchingLines: MutableList<WebLine> = mutableListOf()

		for (lineUuid in cachedUuids) {
			val line = lines[lineUuid] ?: continue
			if (chunkPos !in line.getEndpointChunkPositions()) continue

			matchingLines.add(line)
		}

		return matchingLines
	}

	private fun syncWebPorts(player: ServerPlayer, lines: Collection<WebLine>) {
		val webPorts: MutableList<ItemWebPortData> = mutableListOf()
		val checkedNodes: MutableSet<UUID> = mutableSetOf()

		for (line in lines) {
			addWebPortData(line.firstNode, checkedNodes, webPorts)
			addWebPortData(line.secondNode, checkedNodes, webPorts)
		}

		if (webPorts.isNotEmpty()) {
			SyncItemWebPortsPacket(webPorts).messagePlayer(player)
		}
	}

	private fun addWebPortData(
		node: WebNode,
		checkedNodes: MutableSet<UUID>,
		webPorts: MutableList<ItemWebPortData>
	) {
		if (node !is WebBlockAnchor) return
		if (!checkedNodes.add(node.uuid)) return

		val webPort = getWebPort(node)
		if (!webPort.isEmpty) webPorts.add(ItemWebPortData(node.uuid, webPort))
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
		sendToNearbyPlayers(level, line, RemoveWebLinePacket(WebLineType.ITEM, line.uuid))
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
		addAnchorToParentLine(line.firstNode)
		addAnchorToParentLine(line.secondNode)

		for (node in nodes.values) {
			if (node !is WebLineAnchor || node.parentLineUuid != line.uuid) continue
			line.addAttachedAnchor(node)
		}
	}

	private fun addAnchorToParentLine(node: WebNode) {
		if (node !is WebLineAnchor) return

		val parentLine = lines[node.parentLineUuid] ?: return
		parentLine.addAttachedAnchor(node)
	}

	private fun addLineToNetwork(line: WebLine) {
		val connectedNetworks: MutableSet<WebNetwork> = mutableSetOf()
		for (connectedLine in getConnectedLines(line)) {
			val network = networksByLineUuid[connectedLine.uuid] ?: continue
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

	private fun getConnectedLines(line: WebLine): Set<WebLine> {
		val connectedLines: MutableSet<WebLine> = mutableSetOf()
		addLinesConnectedAtNode(line.firstNode, connectedLines)
		addLinesConnectedAtNode(line.secondNode, connectedLines)

		for (attachment in line.attachedAnchors) {
			connectedLines.addAll(attachment.anchor.lines)
		}

		connectedLines.remove(line)
		return connectedLines
	}

	private fun addLinesConnectedAtNode(node: WebNode, connectedLines: MutableSet<WebLine>) {
		connectedLines.addAll(node.lines)
		if (node !is WebLineAnchor) return

		val parentLine = lines[node.parentLineUuid] ?: return
		connectedLines.add(parentLine)
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
		val firstParentLineUuid = (line.firstNode as? WebLineAnchor)?.parentLineUuid
		if (firstParentLineUuid == referencedLineUuid) return true

		val secondParentLineUuid = (line.secondNode as? WebLineAnchor)?.parentLineUuid
		return secondParentLineUuid == referencedLineUuid
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
			lines[node.parentLineUuid]?.removeAttachedAnchor(node.uuid)
		}
		if (node is WebBlockAnchor) {
			val webPort = webPortsByNodeUuid.remove(node.uuid) ?: ItemStack.EMPTY
			if (webPort.isEmpty) {
				nodes.remove(nodeUuid)
				return
			}

			Containers.dropItemStack(
				level,
				node.position.x,
				node.position.y,
				node.position.z,
				webPort
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
		val webPorts: MutableList<ItemWebPortData> = mutableListOf()
		for ((nodeUuid, stack) in webPortsByNodeUuid) {
			webPorts.add(ItemWebPortData(nodeUuid, stack))
		}
		val webPortsTag = ItemWebPortData.CODEC
			.listOf()
			.encodeStart(ops, webPorts)
			.getOrThrow()

		tag.put(NODES_TAG, nodesTag)
		tag.put(LINES_TAG, linesTag)
		tag.put(WEB_PORTS_TAG, webPortsTag)
		return tag
	}

	companion object {
		private const val CASCADE_DELAY_TICKS = 2
		private const val SAVED_DATA_NAME = "critterworks_item_webs"
		private const val LEGACY_SAVED_DATA_NAME = "critterworks_webs"
		private const val NODES_TAG = "Nodes"
		private const val LINES_TAG = "Lines"
		private const val WEB_PORTS_TAG = "WebPorts"

		private fun load(tag: CompoundTag, registries: HolderLookup.Provider): ItemWebHandler {
			val savedData = ItemWebHandler()
			val nodesTag = tag.get(NODES_TAG) ?: return savedData
			val linesTag = tag.get(LINES_TAG) ?: return savedData

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
				if (node.uuid in referencedNodeUuids) savedData.nodes[node.uuid] = node
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

			val webPortsTag = tag.get(WEB_PORTS_TAG) ?: nodesTag
			val loadedWebPorts = ItemWebPortData.CODEC
				.listOf()
				.parse(ops, webPortsTag)
				.getOrThrow()
			for (webPort in loadedWebPorts) {
				if (webPort.stack.isEmpty) continue
				if (webPort.nodeUuid !in referencedNodeUuids) continue
				savedData.webPortsByNodeUuid[webPort.nodeUuid] = webPort.stack
			}

			return savedData
		}

		fun get(level: ServerLevel): ItemWebHandler {
			val dataStorage = level.dataStorage
			val savedData = dataStorage.get(FACTORY, SAVED_DATA_NAME)
			if (savedData != null) {
				return savedData
			}

			val legacySavedData = dataStorage.get(FACTORY, LEGACY_SAVED_DATA_NAME)
			if (legacySavedData != null) {
				dataStorage.set(LEGACY_SAVED_DATA_NAME, ItemWebHandler())
				dataStorage.set(SAVED_DATA_NAME, legacySavedData)
				legacySavedData.setDirty()
				return legacySavedData
			}

			return dataStorage.computeIfAbsent(FACTORY, SAVED_DATA_NAME)
		}

		private val FACTORY = Factory(::ItemWebHandler, ::load)
	}
}