package dev.aaronhowser.mods.critterworks.entity.data

import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.world.phys.Vec3
import java.util.*

class ScoochwormPath {

	private val points = ArrayDeque<ScoochwormPathPoint>()

	fun record(headPosition: Vec3, supportDirection: Direction) {
		if (points.isEmpty()) {
			points.addFirst(ScoochwormPathPoint(headPosition, supportDirection))
			return
		}

		val movedFarEnough = points.first.position
			.distanceToSqr(headPosition) > MINIMUM_STEP_DISTANCE_SQUARED
		if (movedFarEnough) {
			points.addFirst(ScoochwormPathPoint(headPosition, supportDirection))
		}

		while (points.size > MAX_PATH_POINTS) {
			points.removeLast()
		}
	}

	fun getPoint(distanceFromHead: Double): ScoochwormPathPoint? {
		var remainingDistance = distanceFromHead
		val iterator = points.iterator()
		var positionCloserToHead = iterator.next()

		// Keep going to more and more distant points
		// until it finds one that's farther
		// then lerp it
		while (iterator.hasNext()) {
			val positionFartherFromHead = iterator.next()
			val segmentLength = positionCloserToHead.position.distanceTo(positionFartherFromHead.position)

			if (segmentLength < remainingDistance || segmentLength <= 0.0) {
				remainingDistance -= segmentLength
				positionCloserToHead = positionFartherFromHead
				continue
			}

			return ScoochwormPathPoint(
				positionCloserToHead.position.lerp(
					positionFartherFromHead.position,
					remainingDistance / segmentLength
				),
				positionCloserToHead.supportDirection
			)
		}

		return null
	}

	fun clear() {
		points.clear()
	}

	fun setPoints(pathPoints: List<ScoochwormPathPoint>) {
		points.clear()

		for (pathPoint in pathPoints) {
			points.addLast(pathPoint)
		}
	}

	fun isEmpty(): Boolean = points.isEmpty()

	fun getPoints(): List<ScoochwormPathPoint> = points.toList()

	fun save(): ListTag {
		val tag = ListTag()

		for (pathPoint in points) {
			val pointTag = CompoundTag()
			pointTag.putDouble(X_TAG, pathPoint.position.x)
			pointTag.putDouble(Y_TAG, pathPoint.position.y)
			pointTag.putDouble(Z_TAG, pathPoint.position.z)
			pointTag.putInt(BOTTOM_TAG, pathPoint.supportDirection.get3DDataValue())
			tag.add(pointTag)
		}

		return tag
	}

	fun load(tag: ListTag) {
		points.clear()

		for (index in tag.indices) {
			val pointTag = tag.getCompound(index)
			val position = Vec3(
				pointTag.getDouble(X_TAG),
				pointTag.getDouble(Y_TAG),
				pointTag.getDouble(Z_TAG)
			)

			val supportDirection = Direction.from3DDataValue(pointTag.getInt(BOTTOM_TAG))
			points.addLast(ScoochwormPathPoint(position, supportDirection))
		}
	}

	companion object {
		private const val MAX_PATH_POINTS = 256
		private const val MINIMUM_STEP_DISTANCE_SQUARED = 0.000001
		private const val X_TAG = "X"
		private const val Y_TAG = "Y"
		private const val Z_TAG = "Z"
		private const val BOTTOM_TAG = "Bottom"
	}
}