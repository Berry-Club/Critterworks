package dev.aaronhowser.mods.critterworks.handler.web.common.line

import dev.aaronhowser.mods.critterworks.handler.web.common.node.WebLineAnchor
import dev.aaronhowser.mods.critterworks.handler.web.common.node.WebNode
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.shapes.CollisionContext
import java.util.*

open class WebLine(
	val uuid: UUID,
	val firstNode: WebNode,
	val secondNode: WebNode
) {

	private val attachmentsByAnchorUuid: MutableMap<UUID, LineAnchorAttachment> = mutableMapOf()

	val attachedAnchors: Collection<LineAnchorAttachment>
		get() = attachmentsByAnchorUuid.values

	val data: WebLineData
		get() = WebLineData(uuid, firstNode.uuid, secondNode.uuid)

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
	}

	fun removeAttachedAnchor(anchorUuid: UUID) {
		attachmentsByAnchorUuid.remove(anchorUuid)
	}

	fun clearAttachedAnchors() {
		if (attachmentsByAnchorUuid.isEmpty()) return

		attachmentsByAnchorUuid.clear()
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

		val firstInvalidation = firstNode.findInvalidation(level, lines, checkedLines)
		if (firstInvalidation != null) return firstInvalidation

		val secondInvalidation = secondNode.findInvalidation(level, lines, checkedLines)
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

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other !is WebLine) return false

		return uuid == other.uuid
			&& firstNode == other.firstNode
			&& secondNode == other.secondNode
	}

	override fun hashCode(): Int {
		var result = uuid.hashCode()
		result = 31 * result + firstNode.hashCode()
		result = 31 * result + secondNode.hashCode()
		return result
	}

}