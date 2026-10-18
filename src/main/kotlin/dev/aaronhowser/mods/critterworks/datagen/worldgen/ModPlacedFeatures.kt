package dev.aaronhowser.mods.critterworks.datagen.worldgen

import dev.aaronhowser.mods.critterworks.Critterworks
import net.minecraft.core.registries.Registries
import net.minecraft.data.worldgen.BootstrapContext
import net.minecraft.data.worldgen.placement.PlacementUtils
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.levelgen.VerticalAnchor
import net.minecraft.world.level.levelgen.placement.*

object ModPlacedFeatures {

	val SCOOCHWORM_APPLE: ResourceKey<PlacedFeature> = key("scoochworm_apple")
	val HOPPING_SPIDER_NEST: ResourceKey<PlacedFeature> = key("hopping_spider_nest")
	val SPARKBUG_BUSH: ResourceKey<PlacedFeature> = key("sparkbug_bush")
	val SCOOCHSTEM_VINE: ResourceKey<PlacedFeature> = key("scoochstem_vine")

	fun bootstrap(context: BootstrapContext<PlacedFeature>) {
		val configuredFeatures = context.lookup(Registries.CONFIGURED_FEATURE)

		context.register(
			SCOOCHWORM_APPLE,
			PlacedFeature(
				configuredFeatures.getOrThrow(ModConfiguredFeatures.SCOOCHWORM_APPLE),
				listOf(
					InSquarePlacement.spread(),
					HeightRangePlacement.uniform(
						VerticalAnchor.bottom(),
						VerticalAnchor.absolute(64)
					),
					BiomeFilter.biome()
				)
			)
		)

		context.register(
			HOPPING_SPIDER_NEST,
			PlacedFeature(
				configuredFeatures.getOrThrow(ModConfiguredFeatures.HOPPING_SPIDER_NEST),
				listOf(
					InSquarePlacement.spread(),
					HeightRangePlacement.uniform(
						VerticalAnchor.bottom(),
						VerticalAnchor.absolute(64)
					),
					BiomeFilter.biome()
				)
			)
		)

		context.register(
			SPARKBUG_BUSH,
			PlacedFeature(
				configuredFeatures.getOrThrow(ModConfiguredFeatures.SPARKBUG_BUSH),
				listOf(
					RarityFilter.onAverageOnceEvery(16),
					InSquarePlacement.spread(),
					PlacementUtils.HEIGHTMAP_WORLD_SURFACE,
					BiomeFilter.biome()
				)
			)
		)

		context.register(
			SCOOCHSTEM_VINE,
			PlacedFeature(
				configuredFeatures.getOrThrow(ModConfiguredFeatures.SCOOCHSTEM_VINE),
				listOf(
					RarityFilter.onAverageOnceEvery(2),
					InSquarePlacement.spread(),
					HeightRangePlacement.uniform(
						VerticalAnchor.bottom(),
						VerticalAnchor.absolute(64)
					),
					BiomeFilter.biome()
				)
			)
		)
	}

	private fun key(name: String): ResourceKey<PlacedFeature> {
		return ResourceKey.create(Registries.PLACED_FEATURE, Critterworks.modResource(name))
	}
}