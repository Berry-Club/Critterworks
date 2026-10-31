package dev.aaronhowser.mods.critterworks.handler.web.item.path

import dev.aaronhowser.mods.critterworks.handler.web.common.line.WebLine
import dev.aaronhowser.mods.critterworks.handler.web.common.node.WebNode
import java.util.*

class WebPathfinder(
	private val lines: Set<WebLine>
) {

	private val pathCache = mutableMapOf<PathKey, WebPath?>()
	private var connectionsByNodeUuid: Map<UUID, List<WebPathSegment>>? = null

	fun findShortestPath(startNode: WebNode, endNode: WebNode): WebPath? {
		val cacheKey = PathKey(startNode.uuid, endNode.uuid)
		if (pathCache.containsKey(cacheKey)) {
			return pathCache[cacheKey]
		}

		val connections = getConnectionsByNodeUuid()
		if (startNode.uuid !in connections || endNode.uuid !in connections) {
			cacheMissingPath(startNode.uuid, endNode.uuid)
			return null
		}

		val path = calculateShortestPath(startNode, endNode, connections)
		if (path == null) {
			cacheMissingPath(startNode.uuid, endNode.uuid)
			return null
		}

		cachePath(path)
		return path
	}

	fun invalidate() {
		pathCache.clear()
		connectionsByNodeUuid = null
	}

	private fun calculateShortestPath(
		startNode: WebNode,
		endNode: WebNode,
		connections: Map<UUID, List<WebPathSegment>>
	): WebPath? {
		if (startNode.uuid == endNode.uuid) {
			return WebPath(startNode, endNode, emptyList(), 0.0)
		}

		val distancesByNodeUuid = mutableMapOf<UUID, Double>(startNode.uuid to 0.0)
		val previousSegmentsByNodeUuid = mutableMapOf<UUID, WebPathSegment>()
		val pendingNodes = PriorityQueue(compareBy(PendingNode::distance))
		pendingNodes.add(PendingNode(startNode.uuid, 0.0))

		while (pendingNodes.isNotEmpty()) {
			val pendingNode = pendingNodes.remove()
			if (pendingNode.distance != distancesByNodeUuid[pendingNode.nodeUuid]) continue
			if (pendingNode.nodeUuid == endNode.uuid) break

			updateNeighborDistances(
				pendingNode,
				connections[pendingNode.nodeUuid].orEmpty(),
				distancesByNodeUuid,
				previousSegmentsByNodeUuid,
				pendingNodes
			)
		}

		val distance = distancesByNodeUuid[endNode.uuid] ?: return null
		return buildPath(startNode, endNode, distance, previousSegmentsByNodeUuid)
	}

	private fun updateNeighborDistances(
		pendingNode: PendingNode,
		connections: List<WebPathSegment>,
		distancesByNodeUuid: MutableMap<UUID, Double>,
		previousSegmentsByNodeUuid: MutableMap<UUID, WebPathSegment>,
		pendingNodes: PriorityQueue<PendingNode>
	) {
		for (segment in connections) {
			val nextNodeUuid = segment.toNode.uuid
			val nextDistance = pendingNode.distance + segment.distance
			val knownDistance = distancesByNodeUuid[nextNodeUuid]
			if (knownDistance != null && knownDistance <= nextDistance) continue

			distancesByNodeUuid[nextNodeUuid] = nextDistance
			previousSegmentsByNodeUuid[nextNodeUuid] = segment
			pendingNodes.add(PendingNode(nextNodeUuid, nextDistance))
		}
	}

	private fun buildPath(
		startNode: WebNode,
		endNode: WebNode,
		distance: Double,
		previousSegmentsByNodeUuid: Map<UUID, WebPathSegment>
	): WebPath? {
		val reversedSegments = mutableListOf<WebPathSegment>()
		var currentNodeUuid = endNode.uuid

		while (currentNodeUuid != startNode.uuid) {
			val segment = previousSegmentsByNodeUuid[currentNodeUuid] ?: return null
			reversedSegments.add(segment)
			currentNodeUuid = segment.fromNode.uuid
		}

		return WebPath(startNode, endNode, reversedSegments.asReversed(), distance)
	}

	private fun getConnectionsByNodeUuid(): Map<UUID, List<WebPathSegment>> {
		val cachedConnections = connectionsByNodeUuid
		if (cachedConnections != null) return cachedConnections

		val connections = mutableMapOf<UUID, MutableList<WebPathSegment>>()
		for (line in lines) {
			addLineConnections(line, connections)
		}

		connectionsByNodeUuid = connections
		return connections
	}

	/** Splits a line at each attached node and adds paths in both directions. */
	private fun addLineConnections(
		line: WebLine,
		connections: MutableMap<UUID, MutableList<WebPathSegment>>
	) {
		val nodesByUuid = mutableMapOf(
			line.firstNode.uuid to (line.firstNode to 0.0),
			line.secondNode.uuid to (line.secondNode to line.length)
		)

		for (attachment in line.attachedAnchors) {
			nodesByUuid[attachment.anchor.uuid] =
				attachment.anchor to attachment.distanceToFirstNode
		}

		connections.getOrPut(line.firstNode.uuid, ::mutableListOf)
		connections.getOrPut(line.secondNode.uuid, ::mutableListOf)

		val orderedNodes = nodesByUuid.values.sortedBy { entry -> entry.second }
		for (nodeIndex in 0 until orderedNodes.lastIndex) {
			val (firstNode, firstDistance) = orderedNodes[nodeIndex]
			val (secondNode, secondDistance) = orderedNodes[nodeIndex + 1]
			val distance = secondDistance - firstDistance
			addConnection(connections, firstNode, secondNode, line, distance)
			addConnection(connections, secondNode, firstNode, line, distance)
		}
	}

	private fun addConnection(
		connections: MutableMap<UUID, MutableList<WebPathSegment>>,
		fromNode: WebNode,
		toNode: WebNode,
		line: WebLine,
		distance: Double
	) {
		val nodeConnections = connections.getOrPut(fromNode.uuid, ::mutableListOf)
		nodeConnections.add(WebPathSegment(fromNode, toNode, line, distance))
	}

	private fun cachePath(path: WebPath) {
		pathCache[PathKey(path.startNode.uuid, path.endNode.uuid)] = path

		val reversedPath = path.reversed()
		pathCache[PathKey(reversedPath.startNode.uuid, reversedPath.endNode.uuid)] = reversedPath
	}

	private fun cacheMissingPath(startNodeUuid: UUID, endNodeUuid: UUID) {
		pathCache[PathKey(startNodeUuid, endNodeUuid)] = null
		pathCache[PathKey(endNodeUuid, startNodeUuid)] = null
	}

	private data class PathKey(
		val startNodeUuid: UUID,
		val endNodeUuid: UUID
	)

	private data class PendingNode(
		val nodeUuid: UUID,
		val distance: Double
	)

}