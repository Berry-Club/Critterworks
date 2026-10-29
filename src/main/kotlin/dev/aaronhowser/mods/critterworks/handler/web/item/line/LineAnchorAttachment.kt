package dev.aaronhowser.mods.critterworks.handler.web.item.line

import dev.aaronhowser.mods.critterworks.handler.web.item.node.WebLineAnchor

data class LineAnchorAttachment(
	val anchor: WebLineAnchor,
	val distanceToFirstNode: Double,
	val distanceToSecondNode: Double
)