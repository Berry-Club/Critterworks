package dev.aaronhowser.mods.critterworks.handler.web.common.node

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import kotlin.math.abs

object WebAnchorSupport {

	/** Checks whether the point touches the requested face of any collision box in the block. */
	fun hasSupport(
		level: Level,
		blockPosition: BlockPos,
		face: Direction,
		contactPosition: Vec3
	): Boolean {
		val collisionShape = level.getBlockState(blockPosition)
			.getCollisionShape(level, blockPosition)
		if (collisionShape.isEmpty) return false

		val localPosition = contactPosition.subtract(Vec3.atLowerCornerOf(blockPosition))
		for (bounds in collisionShape.toAabbs()) {
			if (!isWithinFaceBounds(bounds, localPosition, face.axis)) continue

			val surfaceCoordinate = if (face.axisDirection == Direction.AxisDirection.POSITIVE) {
				getMaximum(bounds, face.axis)
			} else {
				getMinimum(bounds, face.axis)
			}

			val contactCoordinate = getCoordinate(localPosition, face.axis)
			if (abs(surfaceCoordinate - contactCoordinate) <= CONTACT_TOLERANCE) {
				return true
			}
		}

		return false
	}

	private fun isWithinFaceBounds(
		bounds: AABB,
		position: Vec3,
		excludedAxis: Direction.Axis
	): Boolean {
		for (axis in Direction.Axis.entries) {
			if (axis == excludedAxis) continue

			val coordinate = getCoordinate(position, axis)

			if (coordinate < getMinimum(bounds, axis) - CONTACT_TOLERANCE) return false
			if (coordinate > getMaximum(bounds, axis) + CONTACT_TOLERANCE) return false
		}

		return true
	}

	private fun getCoordinate(position: Vec3, axis: Direction.Axis): Double {
		return when (axis) {
			Direction.Axis.X -> position.x
			Direction.Axis.Y -> position.y
			Direction.Axis.Z -> position.z
		}
	}

	private fun getMinimum(bounds: AABB, axis: Direction.Axis): Double {
		return when (axis) {
			Direction.Axis.X -> bounds.minX
			Direction.Axis.Y -> bounds.minY
			Direction.Axis.Z -> bounds.minZ
		}
	}

	private fun getMaximum(bounds: AABB, axis: Direction.Axis): Double {
		return when (axis) {
			Direction.Axis.X -> bounds.maxX
			Direction.Axis.Y -> bounds.maxY
			Direction.Axis.Z -> bounds.maxZ
		}
	}

	private const val CONTACT_TOLERANCE = 1.0e-5
}