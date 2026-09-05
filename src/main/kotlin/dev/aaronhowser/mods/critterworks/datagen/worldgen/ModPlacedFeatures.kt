package dev.aaronhowser.mods.critterworks.datagen.worldgen

import dev.aaronhowser.mods.critterworks.Critterworks
import net.minecraft.core.registries.Registries
import net.minecraft.data.worldgen.BootstrapContext
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.levelgen.VerticalAnchor
import net.minecraft.world.level.levelgen.placement.BiomeFilter
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement
import net.minecraft.world.level.levelgen.placement.InSquarePlacement
import net.minecraft.world.level.levelgen.placement.PlacedFeature

object ModPlacedFeatures {

	val SCOOCHWORM_APPLE: ResourceKey<PlacedFeature> = key("scoochworm_apple")
	val HOPPING_SPIDER_NEST: ResourceKey<PlacedFeature> = key("hopping_spider_nest")

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
	}

	private fun key(name: String): ResourceKey<PlacedFeature> {
		return ResourceKey.create(Registries.PLACED_FEATURE, Critterworks.modResource(name))
	}
}