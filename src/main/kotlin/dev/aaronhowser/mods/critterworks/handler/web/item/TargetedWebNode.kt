package dev.aaronhowser.mods.critterworks.handler.web.item

import dev.aaronhowser.mods.critterworks.handler.web.item.node.WebNode
import java.util.UUID

data class TargetedWebNode(
	val lineUuid: UUID?,
	val node: WebNode
)