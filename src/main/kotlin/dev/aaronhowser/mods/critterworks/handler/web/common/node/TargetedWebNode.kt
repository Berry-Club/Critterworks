package dev.aaronhowser.mods.critterworks.handler.web.common.node

import java.util.UUID

data class TargetedWebNode(
	val lineUuid: UUID?,
	val node: WebNode
)