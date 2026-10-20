package dev.aaronhowser.mods.critterworks.entity.punchprawn.goal

import dev.aaronhowser.mods.critterworks.entity.punchprawn.PunchprawnEntity
import net.minecraft.world.entity.ai.goal.Goal
import java.util.EnumSet

class PunchprawnSitGoal(
	private val punchprawn: PunchprawnEntity
) : Goal() {

	init {
		flags = EnumSet.of(Flag.JUMP, Flag.MOVE)
	}

	override fun canUse(): Boolean {
		return punchprawn.isTame && punchprawn.isOrderedToSit
	}

	override fun canContinueToUse(): Boolean {
		return punchprawn.isTame && punchprawn.isOrderedToSit
	}

	override fun start() {
		punchprawn.navigation.stop()
		punchprawn.setInSittingPose(true)
	}

	override fun stop() {
		punchprawn.setInSittingPose(false)
	}

	override fun tick() {
		punchprawn.navigation.stop()
	}
}