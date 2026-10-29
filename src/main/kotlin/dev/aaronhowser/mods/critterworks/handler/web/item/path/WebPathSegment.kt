package dev.aaronhowser.mods.critterworks.handler.web.item.path

import dev.aaronhowser.mods.critterworks.handler.web.common.line.WebLine
import dev.aaronhowser.mods.critterworks.handler.web.common.node.WebNode

data class WebPathSegment(
	val fromNode: WebNode,
	val toNode: WebNode,
	val line: WebLine,
	val distance: Double
) {

	fun reversed(): WebPathSegment {
		return WebPathSegment(toNode, fromNode, line, distance)
	}

}