package dev.aaronhowser.mods.critterworks.datagen.worldgen

import dev.aaronhowser.mods.critterworks.Critterworks
import dev.aaronhowser.mods.critterworks.registry.ModFeatures
import dev.aaronhowser.mods.critterworks.world.feature.ScoochwormAppleConfiguration
import net.minecraft.core.registries.Registries
import net.minecraft.data.worldgen.BootstrapContext
import net.minecraft.resources.ResourceKey
import net.minecraft.util.valueproviders.UniformInt
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature

object ModConfiguredFeatures {

	val SCOOCHWORM_APPLE: ResourceKey<ConfiguredFeature<*, *>> = key("scoochworm_apple")

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
	}

	private fun key(name: String): ResourceKey<ConfiguredFeature<*, *>> {
		return ResourceKey.create(Registries.CONFIGURED_FEATURE, Critterworks.modResource(name))
	}
}