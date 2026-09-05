package dev.aaronhowser.mods.critterworks.datagen.worldgen

import dev.aaronhowser.mods.critterworks.Critterworks
import dev.aaronhowser.mods.critterworks.registry.ModFeatures
import dev.aaronhowser.mods.critterworks.world.feature.config.HoppingSpiderNestConfiguration
import dev.aaronhowser.mods.critterworks.world.feature.config.ScoochwormAppleConfiguration
import net.minecraft.core.registries.Registries
import net.minecraft.data.worldgen.BootstrapContext
import net.minecraft.resources.ResourceKey
import net.minecraft.util.InclusiveRange
import net.minecraft.util.valueproviders.UniformInt
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature

object ModConfiguredFeatures {

	val SCOOCHWORM_APPLE: ResourceKey<ConfiguredFeature<*, *>> = key("scoochworm_apple")
	val HOPPING_SPIDER_NEST: ResourceKey<ConfiguredFeature<*, *>> = key("hopping_spider_nest")

	fun bootstrap(context: BootstrapContext<ConfiguredFeature<*, *>>) {
		context.register(
			SCOOCHWORM_APPLE,
			ConfiguredFeature(
				ModFeatures.SCOOCHWORM_APPLE.get(),
				ScoochwormAppleConfiguration(
					radius = UniformInt.of(2, 4),
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
					webCount = InclusiveRange(1, 3),
					webDistance = InclusiveRange(2.0, 10.0),
					raySpread = 1.0,
					connectionProgress = InclusiveRange(0.3, 0.7),
					spiders = InclusiveRange(3, 8),
					minimumDepthBelowSurface = 3,
					connectWebs = true
				)
			)
		)
	}

	private fun key(name: String): ResourceKey<ConfiguredFeature<*, *>> {
		return ResourceKey.create(Registries.CONFIGURED_FEATURE, Critterworks.modResource(name))
	}
}