package dev.aaronhowser.mods.critterworks.entity.sproutsnail.goal

import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isBlock
import dev.aaronhowser.mods.critterworks.datagen.tag.ModBlockTagsProvider
import dev.aaronhowser.mods.critterworks.entity.sproutsnail.SproutsnailEntity
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.level.block.BonemealableBlock
import net.minecraft.world.level.pathfinder.Path
import java.util.*

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
		path = findPathToBonemealableBlock()

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

	private fun findPathToBonemealableBlock(): Path? {
		val origin = sproutsnail.blockPosition()
		val candidates = getWeightedBonemealableBlockPositions(origin)

		while (candidates.isNotEmpty()) {
			val position = removeWeightedPosition(candidates)
			val path = sproutsnail.navigation.createPath(position, 0) ?: continue
			if (path.canReach()) return path
		}

		return null
	}

	private fun getWeightedBonemealableBlockPositions(origin: BlockPos): MutableMap<BlockPos, Int> {
		val candidates = mutableMapOf<BlockPos, Int>()
		val minimumPosition = origin.offset(-SEARCH_RADIUS, -VERTICAL_SEARCH_RADIUS, -SEARCH_RADIUS)
		val maximumPosition = origin.offset(SEARCH_RADIUS, VERTICAL_SEARCH_RADIUS, SEARCH_RADIUS)

		for (position in BlockPos.betweenClosed(minimumPosition, maximumPosition)) {
			if (!isValidDestination(position)) continue

			val distance = origin.distManhattan(position)
			if (distance == 0) continue

			candidates[position.immutable()] = distance
		}

		return candidates
	}

	private fun isValidDestination(position: BlockPos): Boolean {
		val level = sproutsnail.level()
		val state = level.getBlockState(position)
		val block = state.block
		if (block !is BonemealableBlock || state.isBlock(ModBlockTagsProvider.SPROUTSNAIL_BLACKLIST)) return false
		if (!block.isValidBonemealTarget(level, position, state)) return false

		val destination = position.bottomCenter
		val offset = destination.subtract(sproutsnail.position())
		return level.noCollision(sproutsnail, sproutsnail.boundingBox.move(offset))
	}

	private fun removeWeightedPosition(positions: MutableMap<BlockPos, Int>): BlockPos {
		var totalWeight = 0

		for (weight in positions.values) {
			totalWeight += weight
		}

		var remainingWeight = sproutsnail.random.nextInt(totalWeight)
		var selectedPosition: BlockPos? = null

		for (entry in positions) {
			remainingWeight -= entry.value

			if (remainingWeight < 0) {
				selectedPosition = entry.key
				break
			}
		}

		val position = selectedPosition ?: positions.keys.first()
		positions.remove(position)
		return position
	}

	companion object {
		private const val MOVEMENT_SPEED = 1.0
		private const val SEARCH_RADIUS = 12
		private const val VERTICAL_SEARCH_RADIUS = 4
		private const val SEARCH_INTERVAL_TICKS = 40
	}
}