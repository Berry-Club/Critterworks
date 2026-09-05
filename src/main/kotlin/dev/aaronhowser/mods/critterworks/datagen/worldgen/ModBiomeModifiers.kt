package dev.aaronhowser.mods.critterworks.datagen.worldgen

import dev.aaronhowser.mods.critterworks.Critterworks
import dev.aaronhowser.mods.critterworks.registry.ModBiomeTags
import net.minecraft.core.HolderSet
import net.minecraft.core.registries.Registries
import net.minecraft.data.worldgen.BootstrapContext
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.levelgen.GenerationStep
import net.neoforged.neoforge.common.world.BiomeModifier
import net.neoforged.neoforge.common.world.BiomeModifiers
import net.neoforged.neoforge.registries.NeoForgeRegistries

object ModBiomeModifiers {

	val ADD_SCOOCHWORM_APPLE: ResourceKey<BiomeModifier> = key("add_scoochworm_apple")

	fun bootstrap(context: BootstrapContext<BiomeModifier>) {
		val biomes = context.lookup(Registries.BIOME)
		val placedFeatures = context.lookup(Registries.PLACED_FEATURE)

		context.register(
			ADD_SCOOCHWORM_APPLE,
			BiomeModifiers.AddFeaturesBiomeModifier(
				biomes.getOrThrow(ModBiomeTags.HAS_SCOOCHWORM_APPLE),
				HolderSet.direct(placedFeatures.getOrThrow(ModPlacedFeatures.SCOOCHWORM_APPLE)),
				GenerationStep.Decoration.VEGETAL_DECORATION
			)
		)
	}

	private fun key(name: String): ResourceKey<BiomeModifier> {
		return ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, Critterworks.modResource(name))
	}
}