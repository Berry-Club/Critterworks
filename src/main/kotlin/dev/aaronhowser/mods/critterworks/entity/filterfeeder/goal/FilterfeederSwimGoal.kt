package dev.aaronhowser.mods.critterworks.entity.filterfeeder.goal

import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isFluid
import dev.aaronhowser.mods.critterworks.entity.filterfeeder.FilterfeederEntity
import net.minecraft.core.BlockPos
import net.minecraft.tags.FluidTags
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal
import net.minecraft.world.phys.Vec3

class FilterfeederSwimGoal(
	private val filterfeeder: FilterfeederEntity
) : RandomSwimmingGoal(filterfeeder, 1.0, 40) {

	override fun canUse(): Boolean {
		if (!filterfeeder.isFollower && !filterfeeder.isWithinFloorRange()) {
			val lowerPosition = getLowerPosition() ?: return false
			wantedX = lowerPosition.x
			wantedY = lowerPosition.y
			wantedZ = lowerPosition.z
			return true
		}

		if (filterfeeder.isFollower) return false

		return super.canUse()
	}

	override fun getPosition(): Vec3? {
		var attempts = 0

		while (attempts < MAXIMUM_POSITION_ATTEMPTS) {
			val position = super.getPosition()
			if (position != null) {
				val blockPosition = BlockPos.containing(position)
				if (filterfeeder.isWithinFloorRange(blockPosition)) return position
			}

			attempts++
		}

		return null
	}

	private fun getLowerPosition(): Vec3? {
		val searchPosition = filterfeeder.blockPosition().mutable()
		var lowerPosition: BlockPos? = null
		var distance = 0

		while (distance < CORRECTION_DISTANCE) {
			searchPosition.move(0, -1, 0)

			if (!filterfeeder
					.level()
					.getFluidState(searchPosition)
					.isFluid(FluidTags.WATER)
			) break

			lowerPosition = searchPosition.immutable()
			distance++
		}

		if (lowerPosition == null) return null

		return Vec3.atCenterOf(lowerPosition)
	}

	companion object {
		private const val MAXIMUM_POSITION_ATTEMPTS = 10
		private const val CORRECTION_DISTANCE = 7
	}
}