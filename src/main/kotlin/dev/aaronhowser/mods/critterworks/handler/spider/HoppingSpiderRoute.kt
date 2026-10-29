package dev.aaronhowser.mods.critterworks.handler.spider

import dev.aaronhowser.mods.critterworks.handler.web.item.path.WebPath
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.world.phys.Vec3
import java.util.*
import kotlin.math.ceil

class HoppingSpiderRoute(
	val targetNodeUuid: UUID,
	val positions: List<Vec3>,
	val startGameTime: Long,
	val durationTicks: Int
) {

	fun matches(targetNodeUuid: UUID, positions: List<Vec3>): Boolean {
		return this.targetNodeUuid == targetNodeUuid && this.positions == positions
	}

	fun getPosition(gameTime: Long, partialTick: Float): Vec3 {
		if (positions.size < 2 || durationTicks <= 0) return positions.last()

		return getPositionAtDistance(getTravelDistance(gameTime, partialTick))
	}

	fun getDirection(gameTime: Long, partialTick: Float): Vec3? {
		if (positions.size < 2) return null

		var remainingDistance = getTravelDistance(gameTime, partialTick)
		var lastDirection: Vec3? = null
		for (index in 1 until positions.size) {
			val start = positions[index - 1]
			val end = positions[index]
			val segment = end.subtract(start)
			val segmentDistance = segment.length()

			if (segmentDistance == 0.0) continue
			lastDirection = segment.scale(1.0 / segmentDistance)

			if (remainingDistance > segmentDistance) {
				remainingDistance -= segmentDistance
				continue
			}

			return lastDirection
		}

		return lastDirection
	}

	private fun getTravelDistance(gameTime: Long, partialTick: Float): Double {
		if (durationTicks <= 0) return getDistance()

		val elapsedTicks = gameTime - startGameTime + partialTick.toDouble()
		val progress = (elapsedTicks / durationTicks).coerceIn(0.0, 1.0)
		return getDistance() * progress
	}

	private fun getPositionAtDistance(distance: Double): Vec3 {
		var remainingDistance = distance
		for (index in 1 until positions.size) {
			val start = positions[index - 1]
			val end = positions[index]
			val segmentDistance = start.distanceTo(end)

			if (segmentDistance == 0.0) continue

			if (remainingDistance > segmentDistance) {
				remainingDistance -= segmentDistance
				continue
			}

			return start.lerp(end, remainingDistance / segmentDistance)
		}

		return positions.last()
	}

	private fun getDistance(): Double {
		var distance = 0.0
		for (index in 1 until positions.size) {
			distance += positions[index - 1].distanceTo(positions[index])
		}

		return distance
	}

	fun save(): CompoundTag {
		val tag = CompoundTag()

		tag.putUUID(TARGET_NODE_UUID_TAG, targetNodeUuid)
		tag.putLong(START_GAME_TIME_TAG, startGameTime)
		tag.putInt(DURATION_TICKS_TAG, durationTicks)
		tag.put(POSITIONS_TAG, savePositions())
		return tag
	}

	private fun savePositions(): ListTag {
		val tag = ListTag()

		for (position in positions) {
			val positionTag = CompoundTag()

			positionTag.putDouble(X_TAG, position.x)
			positionTag.putDouble(Y_TAG, position.y)
			positionTag.putDouble(Z_TAG, position.z)

			tag.add(positionTag)
		}

		return tag
	}

	companion object {
		private const val TARGET_NODE_UUID_TAG = "TargetNodeUuid"
		private const val START_GAME_TIME_TAG = "StartGameTime"
		private const val DURATION_TICKS_TAG = "DurationTicks"
		private const val POSITIONS_TAG = "Positions"
		private const val X_TAG = "X"
		private const val Y_TAG = "Y"
		private const val Z_TAG = "Z"

		fun fromPath(path: WebPath, startGameTime: Long, speed: Double): HoppingSpiderRoute {
			val positions: MutableList<Vec3> = mutableListOf(path.startNode.position)

			for (segment in path.segments) {
				positions.add(segment.toNode.position)
			}

			val durationTicks = ceil(path.distance / speed)
				.toInt()
				.coerceAtLeast(1)

			return HoppingSpiderRoute(
				path.endNode.uuid,
				positions,
				startGameTime,
				durationTicks
			)
		}

		fun load(tag: CompoundTag): HoppingSpiderRoute? {
			if (!tag.hasUUID(TARGET_NODE_UUID_TAG)) return null

			val positions = loadPositions(tag.getList(POSITIONS_TAG, CompoundTag.TAG_COMPOUND.toInt()))
			if (positions.isEmpty()) return null

			return HoppingSpiderRoute(
				tag.getUUID(TARGET_NODE_UUID_TAG),
				positions,
				tag.getLong(START_GAME_TIME_TAG),
				tag.getInt(DURATION_TICKS_TAG)
			)
		}

		private fun loadPositions(tag: ListTag): List<Vec3> {
			val positions: MutableList<Vec3> = mutableListOf()

			for (index in tag.indices) {
				val positionTag = tag.getCompound(index)
				positions.add(
					Vec3(
						positionTag.getDouble(X_TAG),
						positionTag.getDouble(Y_TAG),
						positionTag.getDouble(Z_TAG)
					)
				)
			}

			return positions
		}
	}
}