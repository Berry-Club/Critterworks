package dev.aaronhowser.mods.critterworks.entity.punchprawn.control

import dev.aaronhowser.mods.critterworks.entity.punchprawn.PunchprawnEntity
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl

class PunchprawnMoveControl(
	private val punchprawn: PunchprawnEntity
) : SmoothSwimmingMoveControl(punchprawn, 85, 10, 0.02f, 0.1f, false) {

	override fun setWantedPosition(x: Double, y: Double, z: Double, speed: Double) {
		val adjustedSpeed = if (!punchprawn.isInWater && !punchprawn.isTame) {
			speed * 0.8
		} else {
			speed
		}

		super.setWantedPosition(x, y, z, adjustedSpeed)
	}

	override fun tick() {
		if (punchprawn.isInWater && !punchprawn.isOrderedToSit) {
			punchprawn.deltaMovement = punchprawn.deltaMovement.add(0.0, 0.005, 0.0)
		}

		super.tick()
	}
}