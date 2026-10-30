package dev.aaronhowser.mods.critterworks.handler.web.redstone.node

import dev.aaronhowser.mods.critterworks.handler.web.common.line.WebLine
import dev.aaronhowser.mods.critterworks.handler.web.common.line.WebLineInvalidation
import dev.aaronhowser.mods.critterworks.handler.web.common.node.WebNode
import dev.aaronhowser.mods.critterworks.handler.web.redstone.RedstoneWebLine
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.phys.Vec3
import java.util.*

abstract class Arachnode : WebNode() {
	abstract val itemStack: ItemStack
	abstract val inputLineUuid: UUID

	override fun isLoaded(level: ServerLevel): Boolean {
		val chunkPos = ChunkPos(BlockPos.containing(position))
		return isChunkLoaded(level, chunkPos)
	}

	override fun getInvalidation(
		level: ServerLevel,
		lines: Map<UUID, WebLine>,
		checkedLines: MutableSet<UUID>
	): WebLineInvalidation? {
		return null
	}

	abstract fun canPower(sourceLine: RedstoneWebLine, destinationLine: RedstoneWebLine): Boolean

	open fun getGeneratedPower(
		destinationLine: RedstoneWebLine,
		linePowers: Map<UUID, Int>
	): Int {
		return 0
	}

	fun isInputLine(line: RedstoneWebLine): Boolean {
		return line.uuid == inputLineUuid
	}

	fun getOutputDirection(): Vec3? {
		for (line in lines) {
			if (line !is RedstoneWebLine || isInputLine(line)) continue

			val outputNode = when (this) {
				line.firstNode -> line.secondNode
				line.secondNode -> line.firstNode
				else -> continue
			}
			val outputDirection = outputNode.position.subtract(position)
			if (outputDirection.lengthSqr() == 0.0) return null

			return outputDirection.normalize()
		}

		return null
	}
}