package dev.aaronhowser.mods.critterworks.entity.punchprawn.goal

import dev.aaronhowser.mods.aaron.misc.AaronExtensions.chance
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isFluid
import dev.aaronhowser.mods.critterworks.entity.punchprawn.PunchprawnEntity
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.tags.FluidTags
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal
import net.minecraft.world.entity.ai.util.DefaultRandomPos
import net.minecraft.world.phys.Vec3

class PunchprawnWanderGoal(
	private val punchprawn: PunchprawnEntity
) : RandomSwimmingGoal(punchprawn, 1.0, 40) {

	override fun canUse(): Boolean {
		if (punchprawn.target != null) return false

		if (!isWithinFloorRange()) {
			val lowerPosition = getLowerPosition() ?: return false
			wantedX = lowerPosition.x
			wantedY = lowerPosition.y
			wantedZ = lowerPosition.z
			return true
		}

		return super.canUse()
	}

	override fun canContinueToUse(): Boolean {
		if (punchprawn.target != null) return false

		return super.canContinueToUse()
	}

	override fun getPosition(): Vec3? {
		if (punchprawn.random.chance(WATER_DESTINATION_CHANCE)) {
			return DefaultRandomPos.getPos(punchprawn, HORIZONTAL_DISTANCE, VERTICAL_DISTANCE)
		}

		val waterPosition = getWaterPosition()
		if (waterPosition != null) return waterPosition

		return DefaultRandomPos.getPos(punchprawn, HORIZONTAL_DISTANCE, VERTICAL_DISTANCE)
	}

	private fun getWaterPosition(): Vec3? {
		var attempts = 0

		while (attempts < MAXIMUM_POSITION_ATTEMPTS) {
			val randomPosition = super.getPosition()
			if (randomPosition != null) {
				val floorPosition = findFloorPosition(BlockPos.containing(randomPosition))
				if (floorPosition != null) return floorPosition.bottomCenter
			}

			attempts++
		}

		return null
	}

	private fun findFloorPosition(randomPosition: BlockPos): BlockPos? {
		val currentY = punchprawn.blockPosition().y
		val maximumY = currentY + MAXIMUM_VERTICAL_DISTANCE
		val minimumY = currentY - MAXIMUM_VERTICAL_DISTANCE
		val searchPosition = BlockPos.MutableBlockPos(
			randomPosition.x,
			maximumY,
			randomPosition.z
		)

		while (searchPosition.y >= minimumY) {
			val floorPosition = searchPosition.below()
			val isWater = punchprawn
				.level()
				.getFluidState(searchPosition)
				.isFluid(FluidTags.WATER)
			val isFloor = punchprawn
				.level()
				.getBlockState(floorPosition)
				.isFaceSturdy(punchprawn.level(), floorPosition, Direction.UP)

			if (isWater && isFloor) return searchPosition.immutable()

			searchPosition.move(Direction.DOWN)
		}

		return null
	}

	private fun isWithinFloorRange(): Boolean {
		val searchPosition = punchprawn.blockPosition().mutable()
		var distance = 0

		while (distance <= MAXIMUM_VERTICAL_DISTANCE) {
			val floorPosition = searchPosition.below()
			val floorState = punchprawn
				.level()
				.getBlockState(floorPosition)

			if (floorState.isFaceSturdy(punchprawn.level(), floorPosition, Direction.UP)) return true

			searchPosition.move(Direction.DOWN)
			distance++
		}

		return false
	}

	private fun getLowerPosition(): Vec3? {
		val searchPosition = punchprawn.blockPosition().mutable()
		var lowerPosition: BlockPos? = null
		var distance = 0

		while (distance < CORRECTION_DISTANCE) {
			searchPosition.move(Direction.DOWN)

			if (!punchprawn
					.level()
					.getFluidState(searchPosition)
					.isFluid(FluidTags.WATER)
			) break

			lowerPosition = searchPosition.immutable()
			distance++
		}

		if (lowerPosition == null) return null
		return lowerPosition.center
	}

	companion object {
		private const val WATER_DESTINATION_CHANCE = 0.75f
		private const val HORIZONTAL_DISTANCE = 10
		private const val VERTICAL_DISTANCE = 7
		private const val MAXIMUM_POSITION_ATTEMPTS = 10
		private const val MAXIMUM_VERTICAL_DISTANCE = 5
		private const val CORRECTION_DISTANCE = 7
	}
}