package dev.aaronhowser.mods.critterworks.entity.punchprawn.control

import dev.aaronhowser.mods.critterworks.entity.punchprawn.PunchprawnEntity
import net.minecraft.util.Mth
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.ai.control.MoveControl
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class PunchprawnMoveControl(
	private val punchprawn: PunchprawnEntity
) : MoveControl(punchprawn) {

	private var requestedSpeedModifier = 0.0

	override fun setWantedPosition(x: Double, y: Double, z: Double, speed: Double) {
		requestedSpeedModifier = speed
		super.setWantedPosition(x, y, z, speed)
	}

	override fun strafe(forward: Float, strafe: Float) {
		super.strafe(forward, strafe)
		requestedSpeedModifier = speedModifier
	}

	override fun tick() {
		if (!punchprawn.isInWater) {
			tickWalking()
			return
		}

		tickSwimming()
	}

	private fun tickWalking() {
		val landSpeedMultiplier = if (punchprawn.isTame) {
			TAMED_LAND_SPEED_MULTIPLIER
		} else {
			UNTAMED_LAND_SPEED_MULTIPLIER
		}

		speedModifier = requestedSpeedModifier * landSpeedMultiplier
		super.tick()
	}

	private fun tickSwimming() {
		if (!punchprawn.isOrderedToSit) {
			punchprawn.deltaMovement = punchprawn.deltaMovement.add(0.0, 0.005, 0.0)
		}

		speedModifier = requestedSpeedModifier

		if (operation == Operation.JUMPING) {
			operation = Operation.MOVE_TO
		}

		if (operation != Operation.MOVE_TO || punchprawn.navigation.isDone) {
			punchprawn.speed = 0f
			punchprawn.xxa = 0f
			punchprawn.yya = 0f
			punchprawn.zza = 0f
			return
		}

		val distanceX = wantedX - punchprawn.x
		val distanceY = wantedY - punchprawn.y
		val distanceZ = wantedZ - punchprawn.z
		val distanceSquared = distanceX * distanceX + distanceY * distanceY + distanceZ * distanceZ

		if (distanceSquared < MIN_SPEED_SQUARED) {
			punchprawn.zza = 0f
			return
		}

		val targetYaw = (atan2(distanceZ, distanceX) * DEGREES_PER_RADIAN).toFloat() - 90f
		punchprawn.yRot = rotlerp(punchprawn.yRot, targetYaw, MAXIMUM_YAW_CHANGE)
		punchprawn.yBodyRot = punchprawn.yRot
		punchprawn.yHeadRot = punchprawn.yRot

		val movementSpeed = (speedModifier * punchprawn.getAttributeValue(Attributes.MOVEMENT_SPEED)).toFloat()
		punchprawn.speed = movementSpeed * WATER_SPEED_MULTIPLIER

		val horizontalDistance = sqrt(distanceX * distanceX + distanceZ * distanceZ)
		if (abs(distanceY) > MIN_DIRECTION_DISTANCE || abs(horizontalDistance) > MIN_DIRECTION_DISTANCE) {
			var targetPitch = -(atan2(distanceY, horizontalDistance) * DEGREES_PER_RADIAN).toFloat()
			targetPitch = Mth.clamp(Mth.wrapDegrees(targetPitch), -MAXIMUM_PITCH, MAXIMUM_PITCH)
			punchprawn.xRot = Mth.rotLerp(PITCH_ROTATION_FACTOR, punchprawn.xRot, targetPitch)
		}

		val pitchRadians = punchprawn.xRot * RADIANS_PER_DEGREE
		punchprawn.zza = cos(pitchRadians) * movementSpeed
		punchprawn.yya = -sin(pitchRadians) * movementSpeed
	}

	companion object {
		private const val WATER_SPEED_MULTIPLIER = 0.02f
		private const val UNTAMED_LAND_SPEED_MULTIPLIER = 0.03
		private const val TAMED_LAND_SPEED_MULTIPLIER = 0.25
		private const val MAXIMUM_YAW_CHANGE = 10f
		private const val MAXIMUM_PITCH = 85f
		private const val PITCH_ROTATION_FACTOR = 0.1f
		private const val MIN_SPEED_SQUARED = 2.5000003E-7f
		private const val MIN_DIRECTION_DISTANCE = 1.0E-5
		private const val DEGREES_PER_RADIAN = 180.0 / Math.PI
		private const val RADIANS_PER_DEGREE = (Math.PI / 180.0).toFloat()
	}
}