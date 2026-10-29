package dev.aaronhowser.mods.critterworks.handler.webstone

import dev.aaronhowser.mods.critterworks.handler.web.graph.WebVertex
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.phys.Vec3
import java.util.*

open class WebstoneNode(
	override val uuid: UUID,
	override val position: Vec3
) : WebVertex {

	val lines: Set<WebstoneLine>
		field = mutableSetOf()

	override fun isLoaded(level: ServerLevel): Boolean {
		val chunkPosition = ChunkPos(BlockPos.containing(position))
		return level.hasChunk(chunkPosition.x, chunkPosition.z)
	}

	fun addLine(line: WebstoneLine) {
		lines.add(line)
	}

	fun removeLine(line: WebstoneLine) {
		lines.remove(line)
	}

}