package dev.aaronhowser.mods.critterworks.handler.web.item

import dev.aaronhowser.mods.critterworks.handler.web.common.line.WebLine
import dev.aaronhowser.mods.critterworks.handler.web.common.node.WebNode
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
			pathfinder.invalidate()
		}
	}

	fun addLines(lines: Collection<WebLine>) {
		for (line in lines) {
			addLine(line)
		}
	}

	fun removeLine(line: WebLine) {
		if (lines.remove(line)) {
			pathfinder.invalidate()
		}
	}

	fun findShortestPath(startNode: WebNode, endNode: WebNode): WebPath? {
		return pathfinder.findShortestPath(startNode, endNode)
	}

	fun getNodes(): Set<WebNode> {
		return lines.flatMapTo(mutableSetOf()) { line ->
			listOf(line.firstNode, line.secondNode)
		}
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