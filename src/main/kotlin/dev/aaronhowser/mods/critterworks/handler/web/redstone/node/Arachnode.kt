package dev.aaronhowser.mods.critterworks.handler.web.redstone.node

import dev.aaronhowser.mods.critterworks.handler.web.common.line.WebLine
import dev.aaronhowser.mods.critterworks.handler.web.common.line.WebLineInvalidation
import dev.aaronhowser.mods.critterworks.handler.web.common.node.WebNode
import dev.aaronhowser.mods.critterworks.handler.web.redstone.RedstoneWebLine
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.ChunkPos
import java.util.*

abstract class Arachnode : WebNode() {
	abstract val itemStack: ItemStack

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
}