package dev.aaronhowser.mods.critterworks.handler.web.common.line

enum class WebLineInvalidationReason {
	INVALID_ANCHOR,
	MISSING_LINE,
	CYCLIC_DEPENDENCY,
	OBSTRUCTED
}