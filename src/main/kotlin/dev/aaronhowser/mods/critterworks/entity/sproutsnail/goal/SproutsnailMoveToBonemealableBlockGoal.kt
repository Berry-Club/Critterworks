package dev.aaronhowser.mods.critterworks.entity.sproutsnail.goal

import dev.aaronhowser.mods.critterworks.entity.sproutsnail.SproutsnailEntity
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.level.block.BonemealableBlock
import net.minecraft.world.level.pathfinder.Path
import net.minecraft.world.phys.Vec3
import java.util.EnumSet

class SproutsnailMoveToBonemealableBlockGoal(
	private val sproutsnail: SproutsnailEntity
) : Goal() {

	private var path: Path? = null
	private var nextSearchTick = 0

	init {
		flags = EnumSet.of(Flag.MOVE)
	}

	override fun canUse(): Boolean {
		if (sproutsnail.tickCount < nextSearchTick) return false

		nextSearchTick = sproutsnail.tickCount + SEARCH_INTERVAL_TICKS
		path = findPathToFarthestBonemealableBlock()

		return path != null
	}

	override fun canContinueToUse(): Boolean {
		return !sproutsnail.navigation.isDone
	}

	override fun start() {
		val path = this.path ?: return
		sproutsnail.navigation.moveTo(path, MOVEMENT_SPEED)
	}

	override fun stop() {
		path = null
		sproutsnail.navigation.stop()
	}

	private fun findPathToFarthestBonemealableBlock(): Path? {
		val origin = sproutsnail.blockPosition()
		val candidates = getBonemealableBlockPositions(origin)

		while (candidates.isNotEmpty()) {
			val farthestPosition = removeFarthestPosition(candidates, origin)
			val path = sproutsnail.navigation.createPath(farthestPosition, 0) ?: continue
			if (path.canReach()) return path
		}

		return null
	}

	private fun getBonemealableBlockPositions(origin: BlockPos): MutableList<BlockPos> {
		val candidates = mutableListOf<BlockPos>()
		val minimumPosition = origin.offset(-SEARCH_RADIUS, -VERTICAL_SEARCH_RADIUS, -SEARCH_RADIUS)
		val maximumPosition = origin.offset(SEARCH_RADIUS, VERTICAL_SEARCH_RADIUS, SEARCH_RADIUS)

		for (position in BlockPos.betweenClosed(minimumPosition, maximumPosition)) {
			if (!isValidDestination(position)) continue
			candidates.add(position.immutable())
		}

		return candidates
	}

	private fun isValidDestination(position: BlockPos): Boolean {
		val level = sproutsnail.level()
		val state = level.getBlockState(position)
		val block = state.block
		if (block !is BonemealableBlock) return false
		if (!block.isValidBonemealTarget(level, position, state)) return false

		val destination = Vec3.atBottomCenterOf(position)
		val offset = destination.subtract(sproutsnail.position())
		return level.noCollision(sproutsnail, sproutsnail.boundingBox.move(offset))
	}

	private fun removeFarthestPosition(
		positions: MutableList<BlockPos>,
		origin: BlockPos
	): BlockPos {
		var farthestIndex = 0
		var farthestDistance = Double.NEGATIVE_INFINITY
		var index = 0

		while (index < positions.size) {
			val position = positions[index]
			val distance = position.distSqr(origin)

			if (distance > farthestDistance) {
				farthestIndex = index
				farthestDistance = distance
			}

			index++
		}

		return positions.removeAt(farthestIndex)
	}

	companion object {
		private const val MOVEMENT_SPEED = 1.0
		private const val SEARCH_RADIUS = 12
		private const val VERTICAL_SEARCH_RADIUS = 4
		private const val SEARCH_INTERVAL_TICKS = 40
	}
}