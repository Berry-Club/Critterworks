package dev.aaronhowser.mods.critterworks.world.feature

import dev.aaronhowser.mods.aaron.misc.AaronExtensions.withClickToRunCommand
import dev.aaronhowser.mods.critterworks.config.ServerConfig
import dev.aaronhowser.mods.critterworks.registry.ModBlocks
import dev.aaronhowser.mods.critterworks.world.feature.config.ScoochstemVineConfiguration
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.util.RandomSource
import net.minecraft.world.level.WorldGenLevel
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.RotatedPillarBlock
import net.minecraft.world.level.levelgen.feature.Feature
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext

class ScoochstemVineFeature : Feature<ScoochstemVineConfiguration>(ScoochstemVineConfiguration.CODEC) {

	override fun place(context: FeaturePlaceContext<ScoochstemVineConfiguration>): Boolean {
		val configuration = context.config()
		val level = context.level()
		val seedPosition = findSeedPosition(
			level,
			context.origin(),
			configuration.verticalSearchRange
		) ?: return false

		val random = context.random()
		val initialDirection = Direction.Plane.HORIZONTAL.getRandomDirection(random)
		val targetLength = configuration.length.sample(random)
		val segments = planVine(
			level,
			seedPosition,
			initialDirection,
			targetLength,
			configuration.turnChance,
			random
		)

		if (segments.size < MINIMUM_LENGTH) return false

		placeVine(level, segments)

		if (ServerConfig.CONFIG.sendScoochstemVineTeleportMessage.get()) {
			sendTeleportMessage(level, seedPosition)
		}

		return true
	}

	private fun findSeedPosition(
		level: WorldGenLevel,
		origin: BlockPos,
		verticalSearchRange: Int
	): BlockPos? {
		var position = origin
		var searchedDistance = 0

		while (searchedDistance < verticalSearchRange && !canVineReplace(level, position)) {
			position = position.above()
			searchedDistance++
		}

		if (!canVineReplace(level, position)) return null

		while (searchedDistance < verticalSearchRange * 2 && canVineReplace(level, position.below())) {
			position = position.below()
			searchedDistance++
		}

		return if (hasFloorSupport(level, position)) position else null
	}

	private fun planVine(
		level: WorldGenLevel,
		seedPosition: BlockPos,
		initialDirection: Direction,
		targetLength: Int,
		turnChance: Float,
		random: RandomSource
	): List<VineSegment> {
		val segments: MutableList<VineSegment> = mutableListOf()
		val occupiedPositions: MutableSet<BlockPos> = mutableSetOf()
		var heading = initialDirection

		segments.add(VineSegment(seedPosition, heading.axis))
		occupiedPositions.add(seedPosition)

		while (segments.size < targetLength) {
			heading = chooseHeading(heading, turnChance, random)

			val nextSegments = findNextSegments(
				level,
				segments.last().position,
				heading,
				occupiedPositions
			) ?: break
			if (segments.size + nextSegments.size > targetLength) break

			for (segment in nextSegments) {
				segments.add(segment)
				occupiedPositions.add(segment.position)
			}
		}

		return segments
	}

	private fun chooseHeading(
		heading: Direction,
		turnChance: Float,
		random: RandomSource
	): Direction {
		if (random.nextFloat() >= turnChance) return heading

		return if (random.nextBoolean()) heading.clockWise else heading.counterClockWise
	}

	private fun findNextSegments(
		level: WorldGenLevel,
		currentPosition: BlockPos,
		heading: Direction,
		occupiedPositions: Set<BlockPos>
	): List<VineSegment>? {
		val forwardPosition = currentPosition.relative(heading)

		if (canPlaceSegment(level, forwardPosition, occupiedPositions)) {
			if (hasFloorSupport(level, forwardPosition)) {
				return listOf(VineSegment(forwardPosition, heading.axis))
			}

			val lowerPosition = forwardPosition.below()
			if (
				canPlaceSegment(level, lowerPosition, occupiedPositions)
				&& hasFloorSupport(level, lowerPosition)
			) {
				return listOf(
					VineSegment(forwardPosition, heading.axis),
					VineSegment(lowerPosition, Direction.Axis.Y)
				)
			}
		}

		val upperPosition = currentPosition.above()
		val upperForwardPosition = upperPosition.relative(heading)
		if (
			canPlaceSegment(level, upperPosition, occupiedPositions)
			&& canPlaceSegment(level, upperForwardPosition, occupiedPositions)
			&& hasFloorSupport(level, upperForwardPosition)
		) {
			return listOf(
				VineSegment(upperPosition, Direction.Axis.Y),
				VineSegment(upperForwardPosition, heading.axis)
			)
		}

		return null
	}

	private fun canPlaceSegment(
		level: WorldGenLevel,
		position: BlockPos,
		occupiedPositions: Set<BlockPos>
	): Boolean {
		return position !in occupiedPositions && canVineReplace(level, position)
	}

	private fun canVineReplace(level: WorldGenLevel, position: BlockPos): Boolean {
		if (!level.ensureCanWrite(position)) return false

		val state = level.getBlockState(position)
		return state.canBeReplaced() && state.fluidState.isEmpty
	}

	private fun hasFloorSupport(level: WorldGenLevel, position: BlockPos): Boolean {
		val floorPosition = position.below()
		if (!level.ensureCanWrite(floorPosition)) return false

		return level.getBlockState(floorPosition)
			.isFaceSturdy(level, floorPosition, Direction.UP)
	}

	private fun placeVine(level: WorldGenLevel, segments: List<VineSegment>) {
		for (segment in segments) {
			val state = ModBlocks.SCOOCHSTEM.get()
				.defaultBlockState()
				.setValue(RotatedPillarBlock.AXIS, segment.axis)

			level.setBlock(segment.position, state, Block.UPDATE_CLIENTS)
		}
	}

	private fun sendTeleportMessage(level: WorldGenLevel, position: BlockPos) {
		val message = Component.literal("Scoochstem Vine [${position.x}, ${position.y}, ${position.z}]")
			.withStyle(
				Style.EMPTY
					.withClickToRunCommand("/tp @s ${position.x} ${position.y} ${position.z}")
			)

		level.level.server.playerList.broadcastSystemMessage(message, false)
	}

	private data class VineSegment(
		val position: BlockPos,
		val axis: Direction.Axis
	)

	companion object {
		private const val MINIMUM_LENGTH = 3
	}
}