package dev.aaronhowser.mods.critterworks.handler.web.item.path

import dev.aaronhowser.mods.critterworks.handler.web.common.node.WebNode

data class WebPath(
	val startNode: WebNode,
	val endNode: WebNode,
	val segments: List<WebPathSegment>,
	val distance: Double
) {

	fun reversed(): WebPath {
		val reversedSegments: MutableList<WebPathSegment> = mutableListOf()
		for (segment in segments.asReversed()) {
			reversedSegments.add(segment.reversed())
		}

		return WebPath(endNode, startNode, reversedSegments, distance)
	}

}