package dev.aaronhowser.mods.critterworks.handler.web.redstone.part

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import dev.aaronhowser.mods.critterworks.handler.web.common.line.WebLine
import dev.aaronhowser.mods.critterworks.handler.web.common.node.WebBlockAnchor
import dev.aaronhowser.mods.critterworks.handler.web.common.node.WebNode
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.UUIDUtil
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

	companion object {
		val LEGACY_CODEC: Codec<RedstoneWebLine> = RecordCodecBuilder.create { instance ->
			instance.group(
				UUIDUtil.CODEC
					.fieldOf("uuid")
					.forGetter(RedstoneWebLine::uuid),
				WebBlockAnchor.CODEC
					.fieldOf("first_anchor")
					.forGetter { line -> line.firstNode as WebBlockAnchor },
				WebBlockAnchor.CODEC
					.fieldOf("second_anchor")
					.forGetter { line -> line.secondNode as WebBlockAnchor }
			).apply(instance) { uuid, firstAnchor, secondAnchor ->
				RedstoneWebLine(uuid, firstAnchor, secondAnchor)
			}
		}
	}
}