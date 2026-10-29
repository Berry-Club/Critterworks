package dev.aaronhowser.mods.critterworks.handler.web.common.line

data class WebLineInvalidation(
	val reason: WebLineInvalidationReason,
	val dependencyDepth: Int = 0
)