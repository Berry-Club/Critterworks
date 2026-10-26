package dev.aaronhowser.mods.critterworks.datagen.worldgen

import dev.aaronhowser.mods.critterworks.Critterworks
import dev.aaronhowser.mods.critterworks.datagen.tag.ModBiomeTagsProvider
import dev.aaronhowser.mods.critterworks.registry.ModEntityTypes
import net.minecraft.core.HolderSet
import net.minecraft.core.registries.Registries
import net.minecraft.data.worldgen.BootstrapContext
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.biome.MobSpawnSettings
import net.minecraft.world.level.levelgen.GenerationStep
import net.neoforged.neoforge.common.world.BiomeModifier
import net.neoforged.neoforge.common.world.BiomeModifiers
import net.neoforged.neoforge.registries.NeoForgeRegistries

object ModBiomeModifiers {

	val ADD_FILTERFEEDER: ResourceKey<BiomeModifier> = key("add_filterfeeder")
	val ADD_SPROUTSNAIL: ResourceKey<BiomeModifier> = key("add_sproutsnail")
	val ADD_SPROUTSNAIL_POPULATED_OVERWORLD: ResourceKey<BiomeModifier> = key("add_sproutsnail_populated_overworld")
	val ADD_SPROUTSNAIL_SPARSE_OVERWORLD: ResourceKey<BiomeModifier> = key("add_sproutsnail_sparse_overworld")
	val ADD_NETHER_SPROUTSNAIL: ResourceKey<BiomeModifier> = key("add_nether_sproutsnail")
	val ADD_SCOOCHWORM_APPLE: ResourceKey<BiomeModifier> = key("add_scoochworm_apple")
	val ADD_HOPPING_SPIDER_NEST: ResourceKey<BiomeModifier> = key("add_hopping_spider_nest")
	val ADD_SPARKBUG_BUSH: ResourceKey<BiomeModifier> = key("add_sparkbug_bush")
	val ADD_SCOOCHSTEM_VINE: ResourceKey<BiomeModifier> = key("add_scoochstem_vine")

	fun bootstrap(context: BootstrapContext<BiomeModifier>) {
		val biomes = context.lookup(Registries.BIOME)
		val placedFeatures = context.lookup(Registries.PLACED_FEATURE)

		context.register(
			ADD_FILTERFEEDER,
			BiomeModifiers.AddSpawnsBiomeModifier(
				biomes.getOrThrow(ModBiomeTagsProvider.HAS_FILTERFEEDER),
				listOf(
					MobSpawnSettings.SpawnerData(ModEntityTypes.FILTERFEEDER.get(), 1, 1, 1),
					MobSpawnSettings.SpawnerData(ModEntityTypes.FILTERFEEDER.get(), 3, 2, 2),
					MobSpawnSettings.SpawnerData(ModEntityTypes.FILTERFEEDER.get(), 3, 3, 3),
					MobSpawnSettings.SpawnerData(ModEntityTypes.FILTERFEEDER.get(), 1, 4, 4)
				)
			)
		)

		context.register(
			ADD_SPROUTSNAIL,
			BiomeModifiers.AddSpawnsBiomeModifier(
				biomes.getOrThrow(ModBiomeTagsProvider.HAS_SPROUTSNAIL),
				listOf(MobSpawnSettings.SpawnerData(ModEntityTypes.SPROUTSNAIL.get(), 10, 1, 2))
			)
		)

		context.register(
			ADD_SPROUTSNAIL_POPULATED_OVERWORLD,
			BiomeModifiers.AddSpawnsBiomeModifier(
				biomes.getOrThrow(ModBiomeTagsProvider.HAS_SPROUTSNAIL_POPULATED_OVERWORLD),
				listOf(MobSpawnSettings.SpawnerData(ModEntityTypes.SPROUTSNAIL.get(), 2, 1, 1))
			)
		)

		context.register(
			ADD_SPROUTSNAIL_SPARSE_OVERWORLD,
			BiomeModifiers.AddSpawnsBiomeModifier(
				biomes.getOrThrow(ModBiomeTagsProvider.HAS_SPROUTSNAIL_SPARSE_OVERWORLD),
				listOf(MobSpawnSettings.SpawnerData(ModEntityTypes.SPROUTSNAIL.get(), 1, 1, 1))
			)
		)

		context.register(
			ADD_NETHER_SPROUTSNAIL,
			BiomeModifiers.AddSpawnsBiomeModifier(
				biomes.getOrThrow(ModBiomeTagsProvider.HAS_NETHER_SPROUTSNAIL),
				listOf(MobSpawnSettings.SpawnerData(ModEntityTypes.SPROUTSNAIL.get(), 1, 1, 1))
			)
		)

		context.register(
			ADD_SCOOCHWORM_APPLE,
			BiomeModifiers.AddFeaturesBiomeModifier(
				biomes.getOrThrow(ModBiomeTagsProvider.HAS_SCOOCHWORM_APPLE),
				HolderSet.direct(placedFeatures.getOrThrow(ModPlacedFeatures.SCOOCHWORM_APPLE)),
				GenerationStep.Decoration.VEGETAL_DECORATION
			)
		)

		context.register(
			ADD_HOPPING_SPIDER_NEST,
			BiomeModifiers.AddFeaturesBiomeModifier(
				biomes.getOrThrow(ModBiomeTagsProvider.HAS_HOPPING_SPIDER_NEST),
				HolderSet.direct(placedFeatures.getOrThrow(ModPlacedFeatures.HOPPING_SPIDER_NEST)),
				GenerationStep.Decoration.UNDERGROUND_DECORATION
			)
		)

		context.register(
			ADD_SPARKBUG_BUSH,
			BiomeModifiers.AddFeaturesBiomeModifier(
				biomes.getOrThrow(ModBiomeTagsProvider.HAS_SPARKBUG_BUSH),
				HolderSet.direct(placedFeatures.getOrThrow(ModPlacedFeatures.SPARKBUG_BUSH)),
				GenerationStep.Decoration.VEGETAL_DECORATION
			)
		)

		context.register(
			ADD_SCOOCHSTEM_VINE,
			BiomeModifiers.AddFeaturesBiomeModifier(
				biomes.getOrThrow(ModBiomeTagsProvider.HAS_SCOOCHSTEM_VINE),
				HolderSet.direct(placedFeatures.getOrThrow(ModPlacedFeatures.SCOOCHSTEM_VINE)),
				GenerationStep.Decoration.VEGETAL_DECORATION
			)
		)
	}

	private fun key(name: String): ResourceKey<BiomeModifier> {
		return ResourceKey.create(
			NeoForgeRegistries.Keys.BIOME_MODIFIERS,
			Critterworks.modResource(name)
		)
	}
}