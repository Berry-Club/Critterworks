package dev.aaronhowser.mods.critterworks.datagen.worldgen

import dev.aaronhowser.mods.critterworks.Critterworks
import dev.aaronhowser.mods.critterworks.datagen.tag.ModStructureTagsProvider
import dev.aaronhowser.mods.critterworks.registry.ModEntityTypes
import net.minecraft.core.registries.Registries
import net.minecraft.data.worldgen.BootstrapContext
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.biome.MobSpawnSettings
import net.neoforged.neoforge.common.world.StructureModifier
import net.neoforged.neoforge.common.world.StructureModifiers
import net.neoforged.neoforge.registries.NeoForgeRegistries

object ModStructureModifiers {

	val ADD_PUNCHPRAWN: ResourceKey<StructureModifier> = key("add_punchprawn")

	fun bootstrap(context: BootstrapContext<StructureModifier>) {
		val structures = context.lookup(Registries.STRUCTURE)

		context.register(
			ADD_PUNCHPRAWN,
			StructureModifiers.AddSpawnsStructureModifier(
				structures.getOrThrow(ModStructureTagsProvider.HAS_PUNCHPRAWN),
				listOf(
					MobSpawnSettings.SpawnerData(ModEntityTypes.PUNCHPRAWN.get(), 15, 1, 1),
					MobSpawnSettings.SpawnerData(ModEntityTypes.PUNCHPRAWN.get(), 5, 2, 2)
				)
			)
		)
	}

	private fun key(name: String): ResourceKey<StructureModifier> {
		return ResourceKey.create(
			NeoForgeRegistries.Keys.STRUCTURE_MODIFIERS,
			Critterworks.modResource(name)
		)
	}

}