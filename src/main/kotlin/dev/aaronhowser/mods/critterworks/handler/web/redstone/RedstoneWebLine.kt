package dev.aaronhowser.mods.critterworks.handler.web.redstone

import dev.aaronhowser.mods.critterworks.handler.web.common.line.WebLine
import dev.aaronhowser.mods.critterworks.handler.web.common.node.WebBlockAnchor
import dev.aaronhowser.mods.critterworks.handler.web.common.node.WebNode
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import java.util.*

class RedstoneWebLine(
	uuid: UUID,
	firstNode: WebNode,
	secondNode: WebNode,
	initialPower: Int = 0
) : WebLine(
	uuid,
	firstNode,
	secondNode
) {
	private var transmittedPower = initialPower

	val power: Int
		get() = transmittedPower

	fun updatePower(newPower: Int): Boolean {
		if (transmittedPower == newPower) return false

		transmittedPower = newPower
		return true
	}

	fun connectsTo(position: BlockPos, direction: Direction): Boolean {
		return nodeConnectsTo(firstNode, position, direction)
			|| nodeConnectsTo(secondNode, position, direction)
	}

	private fun nodeConnectsTo(node: WebNode, position: BlockPos, direction: Direction): Boolean {
		if (node !is WebBlockAnchor) return false
		return position == getSignalPosition(node) && direction == node.face
	}

	fun getSignalPosition(anchor: WebBlockAnchor): BlockPos {
		return anchor.blockPos.relative(anchor.face)
	}

}