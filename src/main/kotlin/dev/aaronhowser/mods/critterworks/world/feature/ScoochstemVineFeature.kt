package dev.aaronhowser.mods.critterworks.world.feature

import dev.aaronhowser.mods.aaron.misc.AaronExtensions.random
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.withClickToRunCommand
import dev.aaronhowser.mods.critterworks.config.ServerConfig
import dev.aaronhowser.mods.critterworks.registry.ModBlocks
import dev.aaronhowser.mods.critterworks.world.feature.config.ScoochstemVineConfiguration
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.Holder
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.util.RandomSource
import net.minecraft.world.level.WorldGenLevel
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.biome.BiomeManager
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.RotatedPillarBlock
import net.minecraft.world.level.levelgen.feature.Feature
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext

class ScoochstemVineFeature : Feature<ScoochstemVineConfiguration>(ScoochstemVineConfiguration.CODEC) {

	override fun place(context: FeaturePlaceContext<ScoochstemVineConfiguration>): Boolean {
		val configuration = context.config()
		val level = context.level()
		val random = context.random()

		val seedPosition = findSeedPosition(
			level,
			context.origin(),
			configuration.surfaceSearchRange,
			random
		) ?: return false

		val positions = planVine(
			level,
			seedPosition,
			configuration.length.sample(random),
			configuration.turnChance,
			random
		)

		if (positions.size < MINIMUM_LENGTH) return false

		placeVine(level, positions)

		if (ServerConfig.CONFIG.sendScoochstemVineTeleportMessage.get()) {
			sendTeleportMessage(level, seedPosition.position, positions.size)
		}

		return true
	}

	// Apparently the origin provided to the feature is just a random spot in the area
	// so this looks for an air block nearby and then from there searches for a valid surface
	// to generate the vine on
	// No this comment isn't AI generated it's just annoying
	private fun findSeedPosition(
		level: WorldGenLevel,
		origin: BlockPos,
		surfaceSearchRange: Int,
		random: RandomSource
	): SurfacePosition? {
		val cavePositions = findCavePositions(level, origin, surfaceSearchRange)
		val generationBiomeManager = level.biomeManager
			.withDifferentSource(level::getUncachedNoiseBiome)
		val requiredBiome = generationBiomeManager.getBiome(origin)

		while (cavePositions.isNotEmpty()) {
			val cavePosition = cavePositions.removeAt(random.nextInt(cavePositions.size))
			val surfaces = findSurfaces(
				level,
				cavePosition,
				surfaceSearchRange,
				generationBiomeManager,
				requiredBiome
			)

			if (surfaces.isEmpty()) continue

			val surface = surfaces.random(random)

			val forwardDirection = Direction.entries
				.filter { it.axis != surface.supportDirection.axis }
				.random(random)

			return SurfacePosition(
				surface.position,
				forwardDirection,
				surface.supportDirection
			)
		}

		return null
	}

	private fun findCavePositions(
		level: WorldGenLevel,
		origin: BlockPos,
		surfaceSearchRange: Int
	): MutableList<BlockPos> {
		val cavePositions: MutableList<BlockPos> = mutableListOf()

		if (canVineReplace(level, origin)) {
			cavePositions.add(origin)
		}

		for (distance in 1..surfaceSearchRange) {
			val above = origin.above(distance)
			if (canVineReplace(level, above)) {
				cavePositions.add(above)
			}

			val below = origin.below(distance)
			if (canVineReplace(level, below)) {
				cavePositions.add(below)
			}
		}

		return cavePositions
	}

	private fun findSurfaces(
		level: WorldGenLevel,
		cavePosition: BlockPos,
		surfaceSearchRange: Int,
		generationBiomeManager: BiomeManager,
		requiredBiome: Holder<Biome>
	): List<Surface> {
		val surfaces: MutableList<Surface> = mutableListOf()

		for (supportDirection in Direction.entries) {
			val surface = findSurface(
				level,
				cavePosition,
				supportDirection,
				surfaceSearchRange,
				generationBiomeManager,
				requiredBiome
			) ?: continue

			surfaces.add(surface)
		}

		return surfaces
	}

	private fun findSurface(
		level: WorldGenLevel,
		origin: BlockPos,
		supportDirection: Direction,
		surfaceSearchRange: Int,
		generationBiomeManager: BiomeManager,
		requiredBiome: Holder<Biome>
	): Surface? {
		var surfacePosition = origin

		for (distance in 1..surfaceSearchRange) {
			surfacePosition = surfacePosition.relative(supportDirection)
			if (!level.ensureCanWrite(surfacePosition)) return null

			val surfaceState = level.getBlockState(surfacePosition)
			if (surfaceState.canBeReplaced() && surfaceState.fluidState.isEmpty) continue
			if (!surfaceState.isFaceSturdy(level, surfacePosition, supportDirection.opposite)) return null

			val vinePosition = surfacePosition.relative(supportDirection.opposite)
			if (!canVineReplace(level, vinePosition)) return null
			if (generationBiomeManager.getBiome(vinePosition) != requiredBiome) return null

			return Surface(vinePosition, supportDirection)
		}

		return null
	}

	private fun planVine(
		level: WorldGenLevel,
		seedPosition: SurfacePosition,
		targetLength: Int,
		turnChance: Float,
		random: RandomSource
	): List<BlockPos> {
		val positions: MutableList<BlockPos> = mutableListOf(seedPosition.position)
		val occupiedPositions: MutableSet<BlockPos> = mutableSetOf(seedPosition.position)
		var currentPosition = seedPosition

		while (positions.size < targetLength) {
			val step = chooseStep(
				level,
				currentPosition,
				occupiedPositions,
				turnChance,
				random
			) ?: break

			if (positions.size + step.positions.size > targetLength) break

			for (position in step.positions) {
				positions.add(position)
				occupiedPositions.add(position)
			}

			currentPosition = step.destination
		}

		return positions
	}

	private fun chooseStep(
		level: WorldGenLevel,
		current: SurfacePosition,
		occupiedPositions: Set<BlockPos>,
		turnChance: Float,
		random: RandomSource
	): VineStep? {
		if (random.nextFloat() < turnChance) {
			val firstTurnIsClockwise = random.nextBoolean()
			val firstTurn = turnAlongSurface(current, firstTurnIsClockwise)
			val firstTurnStep = moveForward(level, firstTurn, occupiedPositions)
			if (firstTurnStep != null) return firstTurnStep

			val secondTurn = turnAlongSurface(current, !firstTurnIsClockwise)
			val secondTurnStep = moveForward(level, secondTurn, occupiedPositions)
			if (secondTurnStep != null) return secondTurnStep
		}

		return moveForward(level, current, occupiedPositions)
	}

	private fun moveForward(
		level: WorldGenLevel,
		current: SurfacePosition,
		occupiedPositions: Set<BlockPos>
	): VineStep? {
		val forwardPosition = current.position.relative(current.forwardDirection)

		if (canPlaceSegment(level, forwardPosition, occupiedPositions)) {
			if (hasSupport(level, forwardPosition, current.supportDirection)) {
				return VineStep(
					listOf(forwardPosition),
					current.copy(position = forwardPosition)
				)
			}

			val withSupportPosition = forwardPosition.relative(current.supportDirection)
			if (!canPlaceSegment(level, withSupportPosition, occupiedPositions)) return null

			if (hasSupport(level, withSupportPosition, current.supportDirection)) {
				return VineStep(
					listOf(forwardPosition, withSupportPosition),
					current.copy(position = withSupportPosition)
				)
			}

			val newSupportDirection = current.forwardDirection.opposite
			if (!hasSupport(level, withSupportPosition, newSupportDirection)) return null

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
		if (!canPlaceSegment(level, oppositeSupportPosition, occupiedPositions)) return null

		val oppositeSupportForwardPosition = oppositeSupportPosition.relative(current.forwardDirection)
		if (
			canPlaceSegment(level, oppositeSupportForwardPosition, occupiedPositions)
			&& hasSupport(level, oppositeSupportForwardPosition, current.supportDirection)
		) {
			return VineStep(
				listOf(oppositeSupportPosition, oppositeSupportForwardPosition),
				current.copy(position = oppositeSupportForwardPosition)
			)
		}

		if (!hasSupport(level, oppositeSupportPosition, current.forwardDirection)) return null

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

	private fun canPlaceSegment(
		level: WorldGenLevel,
		position: BlockPos,
		occupiedPositions: Set<BlockPos>
	): Boolean {
		return position !in occupiedPositions && canVineReplace(level, position)
	}

	private fun canVineReplace(level: WorldGenLevel, position: BlockPos): Boolean {
		if (level.isOutsideBuildHeight(position)) return false
		if (!level.ensureCanWrite(position)) return false

		val state = level.getBlockState(position)
		return state.canBeReplaced() && state.fluidState.isEmpty
	}

	private fun hasSupport(
		level: WorldGenLevel,
		position: BlockPos,
		supportDirection: Direction
	): Boolean {
		val supportPosition = position.relative(supportDirection)
		if (!level.ensureCanWrite(supportPosition)) return false

		return level.getBlockState(supportPosition)
			.isFaceSturdy(level, supportPosition, supportDirection.opposite)
	}

	private fun placeVine(level: WorldGenLevel, positions: List<BlockPos>) {
		for (index in positions.indices) {
			val state = ModBlocks.SCOOCHSTEM.get()
				.defaultBlockState()
				.setValue(RotatedPillarBlock.AXIS, getSegmentAxis(positions, index))

			level.setBlock(positions[index], state, Block.UPDATE_CLIENTS)
		}
	}

	private fun getSegmentAxis(positions: List<BlockPos>, index: Int): Direction.Axis {
		val movement = if (index == 0) {
			positions[1].subtract(positions[0])
		} else {
			positions[index].subtract(positions[index - 1])
		}

		val direction = Direction.fromDelta(movement.x, movement.y, movement.z)
			?: error("Scoochstem vine positions must be adjacent")

		return direction.axis
	}

	private fun sendTeleportMessage(level: WorldGenLevel, position: BlockPos, length: Int) {
		val message = Component.literal("Scoochstem Vine ($length)")
			.withStyle(
				Style.EMPTY
					.withClickToRunCommand("/tp @s ${position.x} ${position.y} ${position.z}")
			)

		level.level.server.playerList.broadcastSystemMessage(message, false)
	}

	private data class SurfacePosition(
		val position: BlockPos,
		val forwardDirection: Direction,
		val supportDirection: Direction
	) {
		init {
			require(forwardDirection.axis != supportDirection.axis)
		}
	}

	private data class Surface(
		val position: BlockPos,
		val supportDirection: Direction
	)

	private data class VineStep(
		val positions: List<BlockPos>,
		val destination: SurfacePosition
	)

	companion object {
		private const val MINIMUM_LENGTH = 3
	}
}