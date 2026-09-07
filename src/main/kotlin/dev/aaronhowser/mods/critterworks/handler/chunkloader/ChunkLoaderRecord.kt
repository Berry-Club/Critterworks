package dev.aaronhowser.mods.critterworks.handler.chunkloader

import net.minecraft.world.level.ChunkPos
import java.util.UUID

data class ChunkLoaderRecord(
	val attachmentUuid: UUID,
	val placerUuid: UUID,
	val headUuid: UUID,
	val headChunk: ChunkPos
)