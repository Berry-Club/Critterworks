package dev.aaronhowser.mods.critterworks.entity.filterfeeder.control

import dev.aaronhowser.mods.critterworks.entity.filterfeeder.FilterfeederEntity
import net.minecraft.util.Mth
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.ai.control.MoveControl
import net.neoforged.neoforge.common.NeoForgeMod

class FilterfeederMoveControl(
	private val filterfeeder: FilterfeederEntity
) : MoveControl(filterfeeder) {

	override fun tick() {
		if (filterfeeder.isEyeInFluidType(NeoForgeMod.WATER_TYPE.value()) && shouldApplyBuoyancy()) {
			filterfeeder.deltaMovement = filterfeeder.deltaMovement.add(0.0, 0.005, 0.0)
		}

		if (operation != Operation.MOVE_TO || filterfeeder.navigation.isDone) {
			filterfeeder.speed = 0f
			return
		}

		val targetSpeed = (speedModifier * filterfeeder.getAttributeValue(Attributes.MOVEMENT_SPEED)).toFloat()
		filterfeeder.speed = Mth.lerp(0.125f, filterfeeder.speed, targetSpeed)

		val horizontalDifference = wantedX - filterfeeder.x
		val targetY = if (filterfeeder.isWithinFloorRange()) {
			wantedY
		} else {
			minOf(wantedY, filterfeeder.y - 1.0)
		}

		val verticalDifference = targetY - filterfeeder.y
		val depthDifference = wantedZ - filterfeeder.z

		if (verticalDifference != 0.0) {
			val distance = Math.sqrt(
				horizontalDifference * horizontalDifference
					+ verticalDifference * verticalDifference
					+ depthDifference * depthDifference
			)

			filterfeeder.deltaMovement = filterfeeder.deltaMovement.add(
				0.0,
				filterfeeder.speed * (verticalDifference / distance) * 0.1,
				0.0
			)
		}

		if (horizontalDifference == 0.0 && depthDifference == 0.0) return

		val targetRotation = (Mth.atan2(depthDifference, horizontalDifference) * 180f / Math.PI).toFloat() - 90f
		filterfeeder.yRot = rotlerp(filterfeeder.yRot, targetRotation, 90f)
		filterfeeder.yBodyRot = filterfeeder.yRot
	}

	private fun shouldApplyBuoyancy(): Boolean {
		if (!filterfeeder.navigation.isDone) return true

		return filterfeeder.isWithinFloorRange(
			maximumDistance = FilterfeederEntity.MAXIMUM_FLOOR_DISTANCE - 1
		)
	}
}