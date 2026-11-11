package dev.aaronhowser.mods.critterworks.entity.punchprawn.control

import dev.aaronhowser.mods.critterworks.entity.control.AmphibiousMoveControl
import dev.aaronhowser.mods.critterworks.entity.punchprawn.PunchprawnEntity

class PunchprawnMoveControl(
	private val punchprawn: PunchprawnEntity
) : AmphibiousMoveControl(punchprawn) {

	override fun shouldApplyBuoyancy(): Boolean = !punchprawn.isOrderedToSit
}