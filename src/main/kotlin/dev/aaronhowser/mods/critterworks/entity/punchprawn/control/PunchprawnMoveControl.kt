package dev.aaronhowser.mods.critterworks.entity.punchprawn.control

import dev.aaronhowser.mods.critterworks.entity.punchprawn.PunchprawnEntity
import net.minecraft.util.Mth
import net.minecraft.world.entity.ai.control.MoveControl
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class PunchprawnMoveControl(
	private val punchprawn: PunchprawnEntity
) : MoveControl(punchprawn) {

	override fun tick() {
		if (!punchprawn.isInWater) {
			super.tick()
			return
		}

		if (!punchprawn.isOrderedToSit) {
			punchprawn.deltaMovement = punchprawn.deltaMovement.add(0.0, 0.005, 0.0)
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

		if (distanceSquared < MIN_SPEED_SQR) {
			punchprawn.speed = 0f
			punchprawn.zza = 0f
			return
		}

		val targetYaw = (Mth.atan2(distanceZ, distanceX) * Mth.RAD_TO_DEG).toFloat() - 90f
		punchprawn.yRot = rotlerp(punchprawn.yRot, targetYaw, 90f)
		punchprawn.yBodyRot = punchprawn.yRot
		punchprawn.yHeadRot = punchprawn.yRot

		val horizontalDistance = sqrt(distanceX * distanceX + distanceZ * distanceZ)
		if (abs(distanceY) > 1.0E-5 || abs(horizontalDistance) > 1.0E-5) {
			var targetPitch = -(Mth.atan2(distanceY, horizontalDistance) * Mth.RAD_TO_DEG).toFloat()
			targetPitch = Mth.clamp(Mth.wrapDegrees(targetPitch), -85f, 85f)
			punchprawn.xRot = rotlerp(punchprawn.xRot, targetPitch, 5f)
		}

		val movementInput = speedModifier.toFloat()
		val pitchRadians = punchprawn.xRot * Mth.DEG_TO_RAD
		punchprawn.speed = movementInput
		punchprawn.xxa = 0f
		punchprawn.zza = cos(pitchRadians) * movementInput
		punchprawn.yya = -sin(pitchRadians) * movementInput
	}
}