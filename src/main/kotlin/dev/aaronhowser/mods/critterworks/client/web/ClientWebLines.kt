package dev.aaronhowser.mods.critterworks.client.web

import dev.aaronhowser.mods.critterworks.handler.web.WebLineType
import dev.aaronhowser.mods.critterworks.handler.web.common.line.WebLine
import dev.aaronhowser.mods.critterworks.handler.web.common.line.WebLineData
import dev.aaronhowser.mods.critterworks.handler.web.common.interaction.TargetedWebNode
import dev.aaronhowser.mods.critterworks.handler.web.common.node.WebLineAnchor
import dev.aaronhowser.mods.critterworks.handler.web.common.node.WebNode
import dev.aaronhowser.mods.critterworks.handler.web.redstone.RedstoneWebLine
import dev.aaronhowser.mods.critterworks.handler.web.redstone.node.Arachnode
import dev.aaronhowser.mods.critterworks.handler.web.redstone.node.ArachnodePort
import java.util.*

object ClientWebLines {

	private val linesByType: MutableMap<WebLineType, MutableMap<UUID, WebLine>> = mutableMapOf()
	private val nodesByType: MutableMap<WebLineType, MutableMap<UUID, WebNode>> = mutableMapOf()
	private val arachnodesByUuid: MutableMap<UUID, Arachnode> = mutableMapOf()

	fun getLines(): List<WebLine> {
		val lines: MutableList<WebLine> = mutableListOf()
		for (typedLines in linesByType.values) lines.addAll(typedLines.values)
		return lines
	}

	fun getLines(type: WebLineType): List<WebLine> {
		return getLineMap(type).values.toList()
	}

	fun getNodes(type: WebLineType): List<WebNode> {
		return getNodeMap(type).values.toList()
	}

	fun getArachnodes(): List<Arachnode> {
		return arachnodesByUuid.values.toList()
	}

	fun getArachnodePorts(): List<ArachnodePort> {
		val ports: MutableList<ArachnodePort> = mutableListOf()
		for (node in getNodeMap(WebLineType.REDSTONE).values) {
			if (node is ArachnodePort) ports.add(node)
		}

		return ports
	}

	fun getType(target: TargetedWebNode): WebLineType? {
		for (type in WebLineType.entries) {
			val lineUuid = target.lineUuid
			if (lineUuid != null && lineUuid in getLineMap(type)) return type
			if (target.node.uuid in getNodeMap(type)) return type
		}

		return null
	}

	fun addLines(
		type: WebLineType,
		newArachnodes: List<Arachnode>,
		newNodes: List<WebNode>,
		newLines: List<WebLineData>
	) {
		val newNodesByUuid: MutableMap<UUID, WebNode> = mutableMapOf()
		for (node in newNodes) newNodesByUuid[node.uuid] = node
		val storedNodes = getNodeMap(type)
		for (newArachnode in newArachnodes) {
			var arachnode = arachnodesByUuid[newArachnode.uuid]
			if (arachnode == null) {
				arachnode = newArachnode
				arachnodesByUuid[arachnode.uuid] = arachnode
			}

			for (port in arachnode.getPorts()) {
				var storedPort = storedNodes[port.uuid]
				if (storedPort == null) {
					storedPort = port
					storedNodes[port.uuid] = storedPort
				}

				newNodesByUuid[port.uuid] = storedPort
			}
		}

		for (lineData in newLines) {
			addLine(type, lineData, newNodesByUuid)
		}
	}

	private fun addLine(
		type: WebLineType,
		lineData: WebLineData,
		newNodes: Map<UUID, WebNode>
	) {
		val nodes = getNodeMap(type)
		val lines = getLineMap(type)
		val firstNode = getCanonicalNode(nodes, newNodes[lineData.firstNodeUuid] ?: return)
		val secondNode = getCanonicalNode(nodes, newNodes[lineData.secondNodeUuid] ?: return)
		val line = when (type) {
			WebLineType.ITEM -> WebLine(lineData.uuid, firstNode, secondNode)
			WebLineType.REDSTONE -> RedstoneWebLine(lineData.uuid, firstNode, secondNode)
		}
		val previousLine = lines.put(line.uuid, line)

		if (previousLine != null) {
			detachLine(previousLine)
			previousLine.clearAttachedAnchors()
		}

		firstNode.addLine(line)
		secondNode.addLine(line)
		addAnchorToParentLine(lines, firstNode)
		addAnchorToParentLine(lines, secondNode)

		for (node in nodes.values) {
			if (node !is WebLineAnchor || node.parentLineUuid != line.uuid) continue
			line.addAttachedAnchor(node)
		}

		if (previousLine != null) {
			removeNodeIfOrphaned(type, lines, nodes, previousLine.firstNode)
			removeNodeIfOrphaned(type, lines, nodes, previousLine.secondNode)
		}
	}

	fun updateRedstonePower(uuid: UUID, power: Int) {
		val line = getLineMap(WebLineType.REDSTONE)[uuid] as? RedstoneWebLine ?: return
		line.updatePower(power)
	}

	fun removeLine(type: WebLineType, uuid: UUID) {
		val lines = getLineMap(type)
		val nodes = getNodeMap(type)
		val line = lines.remove(uuid) ?: return
		detachLine(line)
		line.clearAttachedAnchors()
		removeNodeIfOrphaned(type, lines, nodes, line.firstNode)
		removeNodeIfOrphaned(type, lines, nodes, line.secondNode)
		removeOrphanedArachnodes(nodes)
	}

	fun removeArachnode(uuid: UUID) {
		val arachnode = arachnodesByUuid.remove(uuid) ?: return
		val nodes = getNodeMap(WebLineType.REDSTONE)
		for (port in arachnode.getPorts()) nodes.remove(port.uuid)
	}

	fun clear() {
		for (typedLines in linesByType.values) {
			for (line in typedLines.values) detachLine(line)
		}

		linesByType.clear()
		nodesByType.clear()
		arachnodesByUuid.clear()
		ClientItemWebPorts.clear()
	}

	private fun detachLine(line: WebLine) {
		line.firstNode.removeLine(line)
		line.secondNode.removeLine(line)
	}

	private fun addAnchorToParentLine(lines: Map<UUID, WebLine>, node: WebNode) {
		if (node !is WebLineAnchor) return

		val parentLine = lines[node.parentLineUuid] ?: return
		parentLine.addAttachedAnchor(node)
	}

	private fun removeNodeIfOrphaned(
		type: WebLineType,
		lines: Map<UUID, WebLine>,
		nodes: MutableMap<UUID, WebNode>,
		node: WebNode
	) {
		if (node.lines.isNotEmpty()) return
		if (nodes[node.uuid] !== node) return
		if (node is ArachnodePort && node.arachnodeUuid in arachnodesByUuid) return

		if (node is WebLineAnchor) {
			lines[node.parentLineUuid]?.removeAttachedAnchor(node.uuid)
		}

		nodes.remove(node.uuid)
		if (type == WebLineType.ITEM) ClientItemWebPorts.remove(node.uuid)
	}

	private fun removeOrphanedArachnodes(nodes: MutableMap<UUID, WebNode>) {
		val removedArachnodeUuids: MutableList<UUID> = mutableListOf()
		for (arachnode in arachnodesByUuid.values) {
			var hasLine = false
			for (port in arachnode.getPorts()) {
				if (port.lines.isEmpty()) continue

				hasLine = true
				break
			}
			if (!hasLine && !arachnode.mount.isMounted) removedArachnodeUuids.add(arachnode.uuid)
		}

		for (arachnodeUuid in removedArachnodeUuids) {
			val arachnode = arachnodesByUuid.remove(arachnodeUuid) ?: continue
			for (port in arachnode.getPorts()) nodes.remove(port.uuid)
		}
	}

	private fun getCanonicalNode(
		nodes: MutableMap<UUID, WebNode>,
		newNode: WebNode
	): WebNode {
		val existingNode = nodes[newNode.uuid]
		if (existingNode != null) return existingNode

		nodes[newNode.uuid] = newNode
		return newNode
	}

	private fun getLineMap(type: WebLineType): MutableMap<UUID, WebLine> {
		return linesByType.getOrPut(type, ::mutableMapOf)
	}

	private fun getNodeMap(type: WebLineType): MutableMap<UUID, WebNode> {
		return nodesByType.getOrPut(type, ::mutableMapOf)
	}
}