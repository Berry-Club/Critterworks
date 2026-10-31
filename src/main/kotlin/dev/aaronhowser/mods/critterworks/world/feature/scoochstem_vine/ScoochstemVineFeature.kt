package dev.aaronhowser.mods.critterworks.world.feature.scoochstem_vine

import dev.aaronhowser.mods.aaron.misc.AaronExtensions.random
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.withClickToRunCommand
import dev.aaronhowser.mods.critterworks.config.ServerConfig
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

		val vinePlan = ScoochstemVinePlan.create(
			level,
			seedPosition,
			configuration.length.sample(random),
			configuration.turnChance,
			random
		)

		if (vinePlan.size < configuration.length.minValue) return false

		vinePlan.place()

		if (ServerConfig.CONFIG.sendScoochstemVineTeleportMessage.get()) {
			sendTeleportMessage(level, seedPosition.position, vinePlan.size)
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
	): ScoochstemVinePlan.SurfacePosition? {
		val cavePositions = findCavePositions(level, origin, surfaceSearchRange)
		val biomeManager = level.biomeManager
			.withDifferentSource(level::getUncachedNoiseBiome)
		val requiredBiome = biomeManager.getBiome(origin)

		while (cavePositions.isNotEmpty()) {
			val cavePosition = cavePositions.random(random)
			cavePositions.remove(cavePosition)
			val surfaces = findSurfaces(
				level,
				cavePosition,
				surfaceSearchRange,
				biomeManager,
				requiredBiome
			)

			if (surfaces.isEmpty()) continue

			val surface = surfaces.random(random)

			val forwardDirection = Direction.entries
				.filter { it.axis != surface.supportDirection.axis }
				.random(random)

			return ScoochstemVinePlan.SurfacePosition(
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
		val cavePositions = mutableListOf<BlockPos>()

		if (ScoochstemVinePlan.canReplace(level, origin)) {
			cavePositions.add(origin)
		}

		for (distance in 1..surfaceSearchRange) {
			val above = origin.above(distance)
			if (ScoochstemVinePlan.canReplace(level, above)) {
				cavePositions.add(above)
			}

			val below = origin.below(distance)
			if (ScoochstemVinePlan.canReplace(level, below)) {
				cavePositions.add(below)
			}
		}

		return cavePositions
	}

	private fun findSurfaces(
		level: WorldGenLevel,
		cavePosition: BlockPos,
		surfaceSearchRange: Int,
		biomeManager: BiomeManager,
		requiredBiome: Holder<Biome>
	): List<Surface> {
		val surfaces = mutableListOf<Surface>()

		for (supportDirection in Direction.entries) {
			val surface = findSurface(
				level,
				cavePosition,
				supportDirection,
				surfaceSearchRange,
				biomeManager,
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
		biomeManager: BiomeManager,
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
			if (!ScoochstemVinePlan.canReplace(level, vinePosition)) return null
			if (biomeManager.getBiome(vinePosition) != requiredBiome) return null

			return Surface(vinePosition, supportDirection)
		}

		return null
	}

	private fun sendTeleportMessage(level: WorldGenLevel, position: BlockPos, length: Int) {
		val message = Component.literal("Scoochstem Vine ($length)")
			.withStyle(
				Style.EMPTY
					.withClickToRunCommand("/tp @s ${position.x} ${position.y} ${position.z}")
			)

		level.level.server.playerList.broadcastSystemMessage(message, false)
	}

	private data class Surface(
		val position: BlockPos,
		val supportDirection: Direction
	)

}