package dev.aaronhowser.mods.critterworks.entity.punchprawn.goal

import dev.aaronhowser.mods.critterworks.entity.punchprawn.PunchprawnEntity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.goal.Goal
import java.util.EnumSet

class PunchprawnFollowOwnerGoal(
	private val punchprawn: PunchprawnEntity,
	private val speedModifier: Double,
	private val startDistance: Float,
	private val stopDistance: Float
) : Goal() {

	private var owner: LivingEntity? = null
	private var pathRecalculationDelay = 0

	init {
		flags = EnumSet.of(Flag.MOVE, Flag.LOOK)
	}

	override fun canUse(): Boolean {
		val currentOwner = punchprawn.owner ?: return false
		if (punchprawn.unableToMoveToOwner()) return false
		if (punchprawn.distanceToSqr(currentOwner) < startDistance * startDistance) return false

		owner = currentOwner
		return true
	}

	override fun canContinueToUse(): Boolean {
		val currentOwner = owner ?: return false
		if (punchprawn.navigation.isDone) return false
		if (punchprawn.unableToMoveToOwner()) return false

		return punchprawn.distanceToSqr(currentOwner) > stopDistance * stopDistance
	}

	override fun start() {
		pathRecalculationDelay = 0
	}

	override fun stop() {
		owner = null
		punchprawn.navigation.stop()
	}

	override fun tick() {
		val currentOwner = owner ?: return
		val shouldTeleport = punchprawn.shouldTryTeleportToOwner()

		if (!shouldTeleport) {
			punchprawn.lookControl.setLookAt(
				currentOwner,
				10f,
				punchprawn.maxHeadXRot.toFloat()
			)
		}

		pathRecalculationDelay--
		if (pathRecalculationDelay > 0) return

		pathRecalculationDelay = adjustedTickDelay(PATH_RECALCULATION_INTERVAL)

		if (shouldTeleport) {
			punchprawn.tryToTeleportToOwner()
		} else {
			punchprawn.navigation.moveTo(currentOwner, speedModifier)
		}
	}

	companion object {
		private const val PATH_RECALCULATION_INTERVAL = 10
	}
}