package dev.aaronhowser.mods.critterworks.handler.web.line

import dev.aaronhowser.mods.critterworks.handler.web.node.WebNode
import java.util.*

object ClientWebLines {

	private val lines: MutableMap<UUID, WebLine> = mutableMapOf()
	private val nodes: MutableMap<UUID, WebNode> = mutableMapOf()

	fun getLines(): List<WebLine> {
		return lines.values.toList()
	}

	fun getNodes(): List<WebNode> {
		return nodes.values.toList()
	}

	fun addLines(newNodes: List<WebNode>, newLines: List<WebLineData>) {
		for (node in newNodes) {
			nodes[node.uuid] = node
		}

		for (lineData in newLines) {
			addLine(lineData)
		}
	}

	private fun addLine(lineData: WebLineData) {
		val firstNode = nodes[lineData.firstNodeUuid] ?: return
		val secondNode = nodes[lineData.secondNodeUuid] ?: return
		val line = WebLine(lineData.uuid, firstNode, secondNode)
		val previousLine = lines.put(line.uuid, line)

		if (previousLine != null) {
			detachLine(previousLine)
		}

		firstNode.addLine(line)
		secondNode.addLine(line)

		if (previousLine != null) {
			removeNodeIfOrphaned(previousLine.firstNode)
			removeNodeIfOrphaned(previousLine.secondNode)
		}
	}

	fun removeLine(uuid: UUID) {
		val line = lines.remove(uuid) ?: return
		detachLine(line)
		removeNodeIfOrphaned(line.firstNode)
		removeNodeIfOrphaned(line.secondNode)
	}

	fun clear() {
		for (line in lines.values) {
			detachLine(line)
		}

		lines.clear()
		nodes.clear()
	}

	private fun detachLine(line: WebLine) {
		line.firstNode.removeLine(line)
		line.secondNode.removeLine(line)
	}

	private fun removeNodeIfOrphaned(node: WebNode) {
		if (node.lines.isNotEmpty()) return
		if (nodes[node.uuid] !== node) return

		nodes.remove(node.uuid)
	}

}