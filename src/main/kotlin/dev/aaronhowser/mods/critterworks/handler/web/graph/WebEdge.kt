package dev.aaronhowser.mods.critterworks.handler.web.graph

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.phys.AABB
import java.util.*

abstract class WebEdge<Vertex : WebVertex> {

	abstract val uuid: UUID
	abstract val firstVertex: Vertex
	abstract val secondVertex: Vertex

	val intersectedChunkPositions: Set<ChunkPos> by lazy(::calculateIntersectedChunkPositions)

	val length: Double by lazy {
		firstVertex.position.distanceTo(secondVertex.position)
	}

	fun isLoaded(level: ServerLevel): Boolean {
		return firstVertex.isLoaded(level) && secondVertex.isLoaded(level)
	}

	fun getEndpointChunkPositions(): Set<ChunkPos> {
		return setOf(
			ChunkPos(BlockPos.containing(firstVertex.position)),
			ChunkPos(BlockPos.containing(secondVertex.position))
		)
	}

	private fun calculateIntersectedChunkPositions(): Set<ChunkPos> {
		val firstChunk = ChunkPos(BlockPos.containing(firstVertex.position))
		val secondChunk = ChunkPos(BlockPos.containing(secondVertex.position))
		val minChunkX = minOf(firstChunk.x, secondChunk.x)
		val maxChunkX = maxOf(firstChunk.x, secondChunk.x)
		val minChunkZ = minOf(firstChunk.z, secondChunk.z)
		val maxChunkZ = maxOf(firstChunk.z, secondChunk.z)
		val minY = minOf(firstVertex.position.y, secondVertex.position.y) - 1.0
		val maxY = maxOf(firstVertex.position.y, secondVertex.position.y) + 1.0
		val chunkPositions: MutableSet<ChunkPos> = mutableSetOf()

		for (chunkX in minChunkX..maxChunkX) {
			for (chunkZ in minChunkZ..maxChunkZ) {
				val chunkPos = ChunkPos(chunkX, chunkZ)
				val bounds = AABB(
					chunkPos.minBlockX.toDouble(),
					minY,
					chunkPos.minBlockZ.toDouble(),
					(chunkPos.maxBlockX + 1).toDouble(),
					maxY,
					(chunkPos.maxBlockZ + 1).toDouble()
				)

				if (bounds.contains(firstVertex.position)
					|| bounds.clip(firstVertex.position, secondVertex.position).isPresent
				) {
					chunkPositions.add(chunkPos)
				}
			}
		}

		return chunkPositions
	}

}