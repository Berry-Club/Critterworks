package dev.aaronhowser.mods.critterworks.handler.web.item

import dev.aaronhowser.mods.critterworks.handler.web.item.line.WebLine
import dev.aaronhowser.mods.critterworks.handler.web.item.node.WebNode
import dev.aaronhowser.mods.critterworks.handler.web.item.path.WebPath
import dev.aaronhowser.mods.critterworks.handler.web.item.path.WebPathfinder
import java.util.*

class WebNetwork(
	val uuid: UUID = UUID.randomUUID()
) {

	val lines: Set<WebLine>
		field = mutableSetOf()

	private val pathfinder = WebPathfinder(lines)

	fun addLine(line: WebLine) {
		if (lines.add(line)) {
			line.network = this
			pathfinder.invalidate()
		}
	}

	fun addLines(lines: Collection<WebLine>) {
		for (line in lines) {
			addLine(line)
		}
	}

	fun removeLine(line: WebLine) {
		if (lines.remove(line) && line.network === this) {
			line.network = null
			pathfinder.invalidate()
		}
	}

	fun findShortestPath(startNode: WebNode, endNode: WebNode): WebPath? {
		return pathfinder.findShortestPath(startNode, endNode)
	}

	fun getNodes(): Set<WebNode> {
		val nodes: MutableSet<WebNode> = mutableSetOf()
		for (line in lines) {
			nodes.add(line.firstNode)
			nodes.add(line.secondNode)
		}

		return nodes
	}

	fun clear() {
		val removedLines = lines.toList()
		for (line in removedLines) {
			removeLine(line)
		}
	}

	fun invalidatePaths() {
		pathfinder.invalidate()
	}
}