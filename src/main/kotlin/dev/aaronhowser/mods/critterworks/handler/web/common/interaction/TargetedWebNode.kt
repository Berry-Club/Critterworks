package dev.aaronhowser.mods.critterworks.handler.web.common.interaction

import dev.aaronhowser.mods.critterworks.handler.web.common.node.WebNode

import java.util.*

data class TargetedWebNode(
	val lineUuid: UUID?,
	val node: WebNode
)