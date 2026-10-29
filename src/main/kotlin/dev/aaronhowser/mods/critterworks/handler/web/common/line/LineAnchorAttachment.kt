package dev.aaronhowser.mods.critterworks.handler.web.common.line

import dev.aaronhowser.mods.critterworks.handler.web.common.node.WebLineAnchor

data class LineAnchorAttachment(
	val anchor: WebLineAnchor,
	val distanceToFirstNode: Double,
	val distanceToSecondNode: Double
)