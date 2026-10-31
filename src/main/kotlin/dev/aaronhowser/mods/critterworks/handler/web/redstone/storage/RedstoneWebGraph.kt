package dev.aaronhowser.mods.critterworks.handler.web.redstone.storage

import dev.aaronhowser.mods.critterworks.handler.web.common.node.WebBlockAnchor
import dev.aaronhowser.mods.critterworks.handler.web.common.node.WebLineAnchor
import dev.aaronhowser.mods.critterworks.handler.web.common.node.WebNode
import dev.aaronhowser.mods.critterworks.handler.web.redstone.RedstoneWebLine
import dev.aaronhowser.mods.critterworks.handler.web.redstone.node.Arachnode
import dev.aaronhowser.mods.critterworks.handler.web.redstone.node.ArachnodeMount
import dev.aaronhowser.mods.critterworks.handler.web.redstone.node.ArachnodePort
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.ChunkPos
import java.util.*

class RedstoneWebGraph {

	val nodes: Map<UUID, WebNode>
		field = mutableMapOf()
	val lines: Map<UUID, RedstoneWebLine>
		field = mutableMapOf()
	val arachnodes: Map<UUID, Arachnode>
		field = mutableMapOf()

	private val lineUuidsByChunk: MutableMap<ChunkPos, MutableSet<UUID>> = mutableMapOf()
	private val lineUuidsBySignalPosition: MutableMap<BlockPos, MutableSet<UUID>> = mutableMapOf()
	private val arachnodeUuidsByChunk: MutableMap<ChunkPos, MutableSet<UUID>> = mutableMapOf()
	private val mountedArachnodeUuidsBySupportChunk: MutableMap<ChunkPos, MutableSet<UUID>> = mutableMapOf()

	fun getNode(uuid: UUID): WebNode? = nodes[uuid]
	fun getCanonicalNode(node: WebNode): WebNode = nodes[node.uuid] ?: node
	fun getLine(uuid: UUID): RedstoneWebLine? = lines[uuid]
	fun getArachnode(uuid: UUID): Arachnode? = arachnodes[uuid]
	fun getOwningArachnode(port: ArachnodePort): Arachnode? = arachnodes[port.arachnodeUuid]

	fun storeNode(node: WebNode) {
		nodes[node.uuid] = node
	}

	fun storeLine(line: RedstoneWebLine) {
		storeNode(line.firstNode)
		storeNode(line.secondNode)
		lines[line.uuid] = line

		addLineReferences(line)
	}

	fun removeLine(line: RedstoneWebLine) {
		lines.remove(line.uuid)
		removeLineReferences(line)
	}

	fun storeArachnode(arachnode: Arachnode) {
		arachnodes[arachnode.uuid] = arachnode
		for (port in arachnode.getPorts()) {
			nodes[port.uuid] = port
		}

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

	fun removeArachnode(arachnode: Arachnode) {
		arachnodes.remove(arachnode.uuid)
		for (port in arachnode.getPorts()) {
			nodes.remove(port.uuid)
		}

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

	fun getLinesNearChunks(chunkPositions: Collection<ChunkPos>): Set<UUID> {
		val lineUuids: MutableSet<UUID> = mutableSetOf()
		for (chunkPosition in chunkPositions) {
			for (chunkX in chunkPosition.x - 1..chunkPosition.x + 1) {
				for (chunkZ in chunkPosition.z - 1..chunkPosition.z + 1) {
					val cachedUuids = lineUuidsByChunk[ChunkPos(chunkX, chunkZ)] ?: continue
					lineUuids.addAll(cachedUuids)
				}
			}
		}

		return lineUuids
	}

	fun getLinesIntersectingChunk(chunkPosition: ChunkPos): List<RedstoneWebLine> {
		val cachedUuids = lineUuidsByChunk[chunkPosition] ?: return emptyList()

		return cachedUuids.mapNotNull { lines[it] }
	}

	fun getArachnodesInChunk(chunkPosition: ChunkPos): List<Arachnode> {
		val arachnodeUuids = arachnodeUuidsByChunk[chunkPosition] ?: return emptyList()

		return arachnodeUuids.mapNotNull { arachnodes[it] }
	}

	fun getMountedArachnodesInSupportChunks(chunkPositions: Collection<ChunkPos>): Set<Arachnode> {
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

	fun getLoadedValidationChunks(): Set<ChunkPos> {
		return lineUuidsByChunk.keys + mountedArachnodeUuidsBySupportChunk.keys
	}

	fun getArachnodes(lines: Collection<RedstoneWebLine>): Set<Arachnode> {
		val matchingArachnodes: MutableSet<Arachnode> = mutableSetOf()
		for (line in lines) {
			addOwningArachnode(line.firstNode, matchingArachnodes)
			addOwningArachnode(line.secondNode, matchingArachnodes)
		}

		return matchingArachnodes
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

		for (chunkPosition in line.intersectedChunkPositions) {
			var lineUuids = lineUuidsByChunk[chunkPosition]
			if (lineUuids == null) {
				lineUuids = mutableSetOf()
				lineUuidsByChunk[chunkPosition] = lineUuids
			}

			lineUuids.add(line.uuid)
		}

		addSignalPositionReference(line, line.firstNode)
		addSignalPositionReference(line, line.secondNode)
	}

	private fun removeLineReferences(line: RedstoneWebLine) {
		line.firstNode.removeLine(line)
		line.secondNode.removeLine(line)

		for (chunkPosition in line.intersectedChunkPositions) {
			val lineUuids = lineUuidsByChunk[chunkPosition] ?: continue
			lineUuids.remove(line.uuid)
			if (lineUuids.isEmpty()) lineUuidsByChunk.remove(chunkPosition)
		}

		removeSignalPositionReference(line, line.firstNode)
		removeSignalPositionReference(line, line.secondNode)
		removeNodeIfOrphaned(line.firstNode)
		removeNodeIfOrphaned(line.secondNode)
		line.clearAttachedAnchors()
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

	private fun removeSignalPositionReference(line: RedstoneWebLine, node: WebNode) {
		if (node !is WebBlockAnchor) return

		val position = line.getSignalPosition(node)
		val lineUuids = lineUuidsBySignalPosition[position] ?: return
		lineUuids.remove(line.uuid)
		if (lineUuids.isEmpty()) lineUuidsBySignalPosition.remove(position)
	}

	private fun removeNodeIfOrphaned(node: WebNode) {
		if (node.lines.isNotEmpty()) return
		if (node is ArachnodePort && node.arachnodeUuid in arachnodes) return

		if (node is WebLineAnchor) {
			lines[node.parentLineUuid]?.removeAttachedAnchor(node.uuid)
		}

		nodes.remove(node.uuid)
	}

	private fun addOwningArachnode(node: WebNode, matchingArachnodes: MutableSet<Arachnode>) {
		if (node !is ArachnodePort) return

		val arachnode = arachnodes[node.arachnodeUuid] ?: return
		matchingArachnodes.add(arachnode)
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

	companion object {
		private const val MAX_POWER = 15
	}
}