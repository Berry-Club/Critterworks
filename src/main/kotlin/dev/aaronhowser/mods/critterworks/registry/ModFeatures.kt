package dev.aaronhowser.mods.critterworks.registry

import dev.aaronhowser.mods.critterworks.Critterworks
import dev.aaronhowser.mods.critterworks.world.feature.HoppingSpiderNestFeature
import dev.aaronhowser.mods.critterworks.world.feature.ScoochwormAppleFeature
import net.minecraft.core.registries.Registries
import net.minecraft.world.level.levelgen.feature.Feature
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister

object ModFeatures {

	val FEATURE_REGISTRY: DeferredRegister<Feature<*>> =
		DeferredRegister.create(Registries.FEATURE, Critterworks.MOD_ID)

	val SCOOCHWORM_APPLE: DeferredHolder<Feature<*>, ScoochwormAppleFeature> =
		FEATURE_REGISTRY.register("scoochworm_apple", ::ScoochwormAppleFeature)

	val HOPPING_SPIDER_NEST: DeferredHolder<Feature<*>, HoppingSpiderNestFeature> =
		FEATURE_REGISTRY.register("hopping_spider_nest", ::HoppingSpiderNestFeature)
}