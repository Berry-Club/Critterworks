package dev.aaronhowser.mods.critterworks.datagen.worldgen

import dev.aaronhowser.mods.critterworks.Critterworks
import dev.aaronhowser.mods.critterworks.registry.ModBlocks
import dev.aaronhowser.mods.critterworks.registry.ModFeatures
import dev.aaronhowser.mods.critterworks.world.feature.config.HoppingSpiderNestConfiguration
import dev.aaronhowser.mods.critterworks.world.feature.config.ScoochwormAppleConfiguration
import net.minecraft.core.registries.Registries
import net.minecraft.data.worldgen.BootstrapContext
import net.minecraft.data.worldgen.placement.PlacementUtils
import net.minecraft.resources.ResourceKey
import net.minecraft.util.InclusiveRange
import net.minecraft.util.valueproviders.UniformInt
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature
import net.minecraft.world.level.levelgen.feature.Feature
import net.minecraft.world.level.levelgen.feature.configurations.RandomPatchConfiguration
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider

object ModConfiguredFeatures {

	val SCOOCHWORM_APPLE: ResourceKey<ConfiguredFeature<*, *>> = key("scoochworm_apple")
	val HOPPING_SPIDER_NEST: ResourceKey<ConfiguredFeature<*, *>> = key("hopping_spider_nest")
	val SPARKBUG_BUSH: ResourceKey<ConfiguredFeature<*, *>> = key("sparkbug_bush")

	fun bootstrap(context: BootstrapContext<ConfiguredFeature<*, *>>) {
		context.register(
			SCOOCHWORM_APPLE,
			ConfiguredFeature(
				ModFeatures.SCOOCHWORM_APPLE.get(),
				ScoochwormAppleConfiguration(
					radius = UniformInt.of(2, 2),
					verticalSearchRange = 24,
					spawnScoochworm = true
				)
			)
		)

		context.register(
			HOPPING_SPIDER_NEST,
			ConfiguredFeature(
				ModFeatures.HOPPING_SPIDER_NEST.get(),
				HoppingSpiderNestConfiguration(
					rayAttempts = 15,
					webCount = InclusiveRange(1, 4),
					webDistance = InclusiveRange(2.0, 10.0),
					raySpread = 1.0,
					connectionProgress = InclusiveRange(0.3, 0.7),
					spiders = InclusiveRange(1, 3),
					minimumDepthBelowSurface = 3,
					connectWebs = true
				)
			)
		)

		context.register(
			SPARKBUG_BUSH,
			ConfiguredFeature(
				Feature.RANDOM_PATCH,
				RandomPatchConfiguration(
					1,
					0,
					0,
					PlacementUtils.onlyWhenEmpty(
						Feature.SIMPLE_BLOCK,
						SimpleBlockConfiguration(
							BlockStateProvider.simple(ModBlocks.SPARKBUG_BUSH.get())
						)
					)
				)
			)
		)
	}

	private fun key(name: String): ResourceKey<ConfiguredFeature<*, *>> {
		return ResourceKey.create(Registries.CONFIGURED_FEATURE, Critterworks.modResource(name))
	}
}