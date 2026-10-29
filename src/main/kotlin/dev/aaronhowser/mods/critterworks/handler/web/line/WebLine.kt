package dev.aaronhowser.mods.critterworks.handler.web.line

import dev.aaronhowser.mods.critterworks.handler.web.WebNetwork
import dev.aaronhowser.mods.critterworks.handler.web.node.WebLineAnchor
import dev.aaronhowser.mods.critterworks.handler.web.node.WebNode
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.shapes.CollisionContext
import java.util.*

data class WebLine(
	val uuid: UUID,
	val firstNode: WebNode,
	val secondNode: WebNode
) {
	var network: WebNetwork? = null

	private val attachmentsByAnchorUuid: MutableMap<UUID, LineAnchorAttachment> = mutableMapOf()

	val attachedAnchors: Collection<LineAnchorAttachment>
		get() = attachmentsByAnchorUuid.values

	val data = WebLineData(uuid, firstNode.uuid, secondNode.uuid)
	val intersectedChunkPositions: Set<ChunkPos> = calculateIntersectedChunkPositions()

	val length: Double by lazy {
		firstNode.position.distanceTo(secondNode.position)
	}

	fun addAttachedAnchor(anchor: WebLineAnchor) {
		attachmentsByAnchorUuid[anchor.uuid] = LineAnchorAttachment(
			anchor,
			anchor.position.distanceTo(firstNode.position),
			anchor.position.distanceTo(secondNode.position)
		)
		network?.invalidatePaths()
	}

	fun removeAttachedAnchor(anchorUuid: UUID) {
		if (attachmentsByAnchorUuid.remove(anchorUuid) != null) {
			network?.invalidatePaths()
		}
	}

	fun clearAttachedAnchors() {
		if (attachmentsByAnchorUuid.isEmpty()) return

		attachmentsByAnchorUuid.clear()
		network?.invalidatePaths()
	}

	fun isLoaded(level: ServerLevel): Boolean {
		return firstNode.isLoaded(level) && secondNode.isLoaded(level)
	}

	fun getInvalidation(
		level: ServerLevel,
		lines: Map<UUID, WebLine>
	): WebLineInvalidation? {
		return getInvalidation(level, lines, mutableSetOf())
	}

	fun getInvalidation(
		level: ServerLevel,
		lines: Map<UUID, WebLine>,
		checkedLines: MutableSet<UUID>
	): WebLineInvalidation? {
		if (!checkedLines.add(uuid)) {
			return WebLineInvalidation(WebLineInvalidationReason.CYCLIC_DEPENDENCY)
		}

		val firstInvalidation = firstNode.getInvalidation(level, lines, checkedLines)
		if (firstInvalidation != null) return firstInvalidation

		val secondInvalidation = secondNode.getInvalidation(level, lines, checkedLines)
		if (secondInvalidation != null) return secondInvalidation

		checkedLines.remove(uuid)

		val clipContext = ClipContext(
			firstNode.position,
			secondNode.position,
			ClipContext.Block.COLLIDER,
			ClipContext.Fluid.NONE,
			CollisionContext.empty()
		)

		val hitResult = level.clip(clipContext)
		if (hitResult.type == HitResult.Type.MISS) return null

		return WebLineInvalidation(WebLineInvalidationReason.OBSTRUCTED)
	}

	fun getEndpointChunkPositions(): Set<ChunkPos> {
		return setOf(
			ChunkPos(BlockPos.containing(firstNode.position)),
			ChunkPos(BlockPos.containing(secondNode.position))
		)
	}

	private fun calculateIntersectedChunkPositions(): Set<ChunkPos> {
		val firstChunk = ChunkPos(BlockPos.containing(firstNode.position))
		val secondChunk = ChunkPos(BlockPos.containing(secondNode.position))
		val minChunkX = minOf(firstChunk.x, secondChunk.x)
		val maxChunkX = maxOf(firstChunk.x, secondChunk.x)
		val minChunkZ = minOf(firstChunk.z, secondChunk.z)
		val maxChunkZ = maxOf(firstChunk.z, secondChunk.z)
		val minY = minOf(firstNode.position.y, secondNode.position.y) - 1.0
		val maxY = maxOf(firstNode.position.y, secondNode.position.y) + 1.0
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

				if (bounds.contains(firstNode.position)
					|| bounds.clip(firstNode.position, secondNode.position).isPresent
				) {
					chunkPositions.add(chunkPos)
				}
			}
		}

		return chunkPositions
	}

}