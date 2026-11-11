package dev.aaronhowser.mods.critterworks.entity.control

import net.minecraft.util.Mth
import net.minecraft.world.entity.PathfinderMob
import net.minecraft.world.entity.ai.control.MoveControl
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

open class AmphibiousMoveControl(
	protected val swimmingMob: PathfinderMob
) : MoveControl(swimmingMob) {

	override fun tick() {
		if (!swimmingMob.isInWater) {
			super.tick()
			return
		}

		if (shouldApplyBuoyancy()) {
			swimmingMob.deltaMovement = swimmingMob.deltaMovement.add(0.0, 0.005, 0.0)
		}

		if (operation != Operation.MOVE_TO || swimmingMob.navigation.isDone) {
			swimmingMob.speed = 0f
			swimmingMob.xxa = 0f
			swimmingMob.yya = 0f
			swimmingMob.zza = 0f
			return
		}

		val distanceX = wantedX - swimmingMob.x
		val distanceY = wantedY - swimmingMob.y
		val distanceZ = wantedZ - swimmingMob.z
		val distanceSquared = distanceX * distanceX + distanceY * distanceY + distanceZ * distanceZ

		if (distanceSquared < MIN_SPEED_SQR) {
			swimmingMob.speed = 0f
			swimmingMob.zza = 0f
			return
		}

		val targetYaw = (Mth.atan2(distanceZ, distanceX) * Mth.RAD_TO_DEG).toFloat() - 90f
		swimmingMob.yRot = rotlerp(swimmingMob.yRot, targetYaw, 90f)
		swimmingMob.yBodyRot = swimmingMob.yRot
		swimmingMob.yHeadRot = swimmingMob.yRot

		val horizontalDistance = sqrt(distanceX * distanceX + distanceZ * distanceZ)
		if (abs(distanceY) > 1.0E-5 || abs(horizontalDistance) > 1.0E-5) {
			var targetPitch = -(Mth.atan2(distanceY, horizontalDistance) * Mth.RAD_TO_DEG).toFloat()
			targetPitch = Mth.clamp(Mth.wrapDegrees(targetPitch), -85f, 85f)
			swimmingMob.xRot = rotlerp(swimmingMob.xRot, targetPitch, 5f)
		}

		val movementInput = speedModifier.toFloat()
		val pitchRadians = swimmingMob.xRot * Mth.DEG_TO_RAD
		swimmingMob.speed = movementInput
		swimmingMob.xxa = 0f
		swimmingMob.zza = cos(pitchRadians) * movementInput
		swimmingMob.yya = -sin(pitchRadians) * movementInput
	}

	protected open fun shouldApplyBuoyancy(): Boolean = true
}