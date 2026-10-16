package dev.aaronhowser.mods.critterworks.world.feature.scoochstem_vine

import dev.aaronhowser.mods.critterworks.registry.ModBlocks
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.util.RandomSource
import net.minecraft.world.level.WorldGenLevel
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.RotatedPillarBlock

class ScoochstemVinePlan private constructor(
	private val level: WorldGenLevel,
	seedPosition: SurfacePosition
) {

	private val positions: MutableList<BlockPos> = mutableListOf(seedPosition.position)
	private val occupiedPositions: MutableSet<BlockPos> = mutableSetOf(seedPosition.position)

	val size: Int
		get() = positions.size

	fun place() {
		for (index in positions.indices) {
			val state = ModBlocks.SCOOCHSTEM.get()
				.defaultBlockState()
				.setValue(RotatedPillarBlock.AXIS, getSegmentAxis(index))

			level.setBlock(positions[index], state, Block.UPDATE_CLIENTS)
		}
	}

	private fun extend(
		seedPosition: SurfacePosition,
		targetLength: Int,
		turnChance: Float,
		random: RandomSource
	) {
		var currentPosition = seedPosition

		while (positions.size < targetLength) {
			val step = chooseStep(currentPosition, turnChance, random) ?: break

			if (positions.size + step.positions.size > targetLength) break

			for (position in step.positions) {
				positions.add(position)
				occupiedPositions.add(position)
			}

			currentPosition = step.destination
		}
	}

	private fun chooseStep(
		current: SurfacePosition,
		turnChance: Float,
		random: RandomSource
	): VineStep? {
		if (random.nextFloat() < turnChance) {
			val firstTurnIsClockwise = random.nextBoolean()
			val firstTurn = turnAlongSurface(current, firstTurnIsClockwise)
			val firstTurnStep = moveForward(firstTurn)
			if (firstTurnStep != null) return firstTurnStep

			val secondTurn = turnAlongSurface(current, !firstTurnIsClockwise)
			val secondTurnStep = moveForward(secondTurn)
			if (secondTurnStep != null) return secondTurnStep
		}

		return moveForward(current)
	}

	private fun moveForward(current: SurfacePosition): VineStep? {
		val forwardPosition = current.position.relative(current.forwardDirection)

		if (canPlaceSegment(forwardPosition)) {
			if (hasSupport(forwardPosition, current.supportDirection)) {
				return VineStep(
					listOf(forwardPosition),
					current.copy(position = forwardPosition)
				)
			}

			val withSupportPosition = forwardPosition.relative(current.supportDirection)
			if (!canPlaceSegment(withSupportPosition)) return null

			if (hasSupport(withSupportPosition, current.supportDirection)) {
				return VineStep(
					listOf(forwardPosition, withSupportPosition),
					current.copy(position = withSupportPosition)
				)
			}

			val newSupportDirection = current.forwardDirection.opposite
			if (!hasSupport(withSupportPosition, newSupportDirection)) return null

			return VineStep(
				listOf(forwardPosition, withSupportPosition),
				SurfacePosition(
					withSupportPosition,
					current.supportDirection,
					newSupportDirection
				)
			)
		}

		val oppositeSupportDirection = current.supportDirection.opposite
		val oppositeSupportPosition = current.position.relative(oppositeSupportDirection)
		if (!canPlaceSegment(oppositeSupportPosition)) return null

		val oppositeSupportForwardPosition = oppositeSupportPosition.relative(current.forwardDirection)
		if (
			canPlaceSegment(oppositeSupportForwardPosition)
			&& hasSupport(oppositeSupportForwardPosition, current.supportDirection)
		) {
			return VineStep(
				listOf(oppositeSupportPosition, oppositeSupportForwardPosition),
				current.copy(position = oppositeSupportForwardPosition)
			)
		}

		if (!hasSupport(oppositeSupportPosition, current.forwardDirection)) return null

		return VineStep(
			listOf(oppositeSupportPosition),
			SurfacePosition(
				oppositeSupportPosition,
				oppositeSupportDirection,
				current.forwardDirection
			)
		)
	}

	private fun turnAlongSurface(
		position: SurfacePosition,
		clockwise: Boolean
	): SurfacePosition {
		val forward = position.forwardDirection.normal
		val support = position.supportDirection.normal
		val first = if (clockwise) support else forward
		val second = if (clockwise) forward else support

		val turnedDirection = Direction.fromDelta(
			first.y * second.z - first.z * second.y,
			first.z * second.x - first.x * second.z,
			first.x * second.y - first.y * second.x
		) ?: error("Forward and support directions must be perpendicular")

		return position.copy(forwardDirection = turnedDirection)
	}

	private fun canPlaceSegment(position: BlockPos): Boolean {
		return position !in occupiedPositions && canReplace(level, position)
	}

	private fun hasSupport(position: BlockPos, supportDirection: Direction): Boolean {
		val supportPosition = position.relative(supportDirection)
		if (!level.ensureCanWrite(supportPosition)) return false

		return level.getBlockState(supportPosition)
			.isFaceSturdy(level, supportPosition, supportDirection.opposite)
	}

	private fun getSegmentAxis(index: Int): Direction.Axis {
		val movement = if (index == 0) {
			positions[1].subtract(positions[0])
		} else {
			positions[index].subtract(positions[index - 1])
		}

		val direction = Direction.fromDelta(movement.x, movement.y, movement.z)
			?: error("Scoochstem vine positions must be adjacent")

		return direction.axis
	}

	data class SurfacePosition(
		val position: BlockPos,
		val forwardDirection: Direction,
		val supportDirection: Direction
	) {
		init {
			require(forwardDirection.axis != supportDirection.axis)
		}
	}

	private data class VineStep(
		val positions: List<BlockPos>,
		val destination: SurfacePosition
	)

	companion object {
		fun canReplace(level: WorldGenLevel, position: BlockPos): Boolean {
			if (level.isOutsideBuildHeight(position)) return false
			if (!level.ensureCanWrite(position)) return false

			val state = level.getBlockState(position)
			return state.canBeReplaced() && state.fluidState.isEmpty
		}

		fun create(
			level: WorldGenLevel,
			seedPosition: SurfacePosition,
			targetLength: Int,
			turnChance: Float,
			random: RandomSource
		): ScoochstemVinePlan {
			val plan = ScoochstemVinePlan(level, seedPosition)
			plan.extend(seedPosition, targetLength, turnChance, random)

			return plan
		}
	}

}