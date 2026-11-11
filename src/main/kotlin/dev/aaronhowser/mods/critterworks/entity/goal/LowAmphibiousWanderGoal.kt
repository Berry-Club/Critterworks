package dev.aaronhowser.mods.critterworks.entity.goal

import dev.aaronhowser.mods.aaron.misc.AaronExtensions.chance
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isFluid
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.tags.FluidTags
import net.minecraft.world.entity.PathfinderMob
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal
import net.minecraft.world.entity.ai.util.DefaultRandomPos
import net.minecraft.world.phys.Vec3

class LowAmphibiousWanderGoal(
	private val wanderingMob: PathfinderMob,
	private val maximumWaterFloorDistance: Int
) : RandomSwimmingGoal(wanderingMob, 1.0, 40) {

	override fun canUse(): Boolean {
		if (wanderingMob.target != null) return false

		if (!isWithinWaterFloorRange()) {
			val lowerPosition = getLowerPosition() ?: return false
			wantedX = lowerPosition.x
			wantedY = lowerPosition.y
			wantedZ = lowerPosition.z
			return true
		}

		return super.canUse()
	}

	override fun canContinueToUse(): Boolean {
		if (wanderingMob.target != null) return false

		return super.canContinueToUse()
	}

	override fun getPosition(): Vec3? {
		if (wanderingMob.random.chance(AMPHIBIOUS_DESTINATION_CHANCE)) {
			return DefaultRandomPos.getPos(wanderingMob, HORIZONTAL_DISTANCE, VERTICAL_DISTANCE)
		}

		val waterPosition = getWaterPosition()
		if (waterPosition != null) return waterPosition

		return DefaultRandomPos.getPos(wanderingMob, HORIZONTAL_DISTANCE, VERTICAL_DISTANCE)
	}

	private fun getWaterPosition(): Vec3? {
		var attempts = 0

		while (attempts < MAXIMUM_POSITION_ATTEMPTS) {
			val randomPosition = super.getPosition()
			if (randomPosition != null) {
				val floorPosition = findWaterFloorPosition(BlockPos.containing(randomPosition))
				if (floorPosition != null) return floorPosition.bottomCenter
			}

			attempts++
		}

		return null
	}

	private fun findWaterFloorPosition(randomPosition: BlockPos): BlockPos? {
		val currentY = wanderingMob.blockPosition().y
		val maximumY = currentY + maximumWaterFloorDistance
		val minimumY = currentY - maximumWaterFloorDistance
		val searchPosition = BlockPos.MutableBlockPos(
			randomPosition.x,
			maximumY,
			randomPosition.z
		)

		while (searchPosition.y >= minimumY) {
			val floorPosition = searchPosition.below()
			val isWater = wanderingMob
				.level()
				.getFluidState(searchPosition)
				.isFluid(FluidTags.WATER)
			val isFloor = wanderingMob
				.level()
				.getBlockState(floorPosition)
				.isFaceSturdy(wanderingMob.level(), floorPosition, Direction.UP)

			if (isWater && isFloor) return searchPosition.immutable()

			searchPosition.move(Direction.DOWN)
		}

		return null
	}

	private fun isWithinWaterFloorRange(): Boolean {
		if (!wanderingMob.isInWater) return true

		val searchPosition = wanderingMob.blockPosition().mutable()
		var distance = 0

		while (distance < maximumWaterFloorDistance) {
			val floorPosition = searchPosition.below()
			val floorState = wanderingMob
				.level()
				.getBlockState(floorPosition)

			if (floorState.isFaceSturdy(wanderingMob.level(), floorPosition, Direction.UP)) return true

			searchPosition.move(Direction.DOWN)
			distance++
		}

		return false
	}

	private fun getLowerPosition(): Vec3? {
		val searchPosition = wanderingMob.blockPosition().mutable()
		var lowerPosition: BlockPos? = null
		var distance = 0

		while (distance < CORRECTION_DISTANCE) {
			searchPosition.move(Direction.DOWN)

			if (!wanderingMob
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
		private const val AMPHIBIOUS_DESTINATION_CHANCE = 0.75f
		private const val HORIZONTAL_DISTANCE = 10
		private const val VERTICAL_DISTANCE = 7
		private const val MAXIMUM_POSITION_ATTEMPTS = 10
		private const val CORRECTION_DISTANCE = 7
	}
}