package dev.aaronhowser.mods.critterworks.registry

import dev.aaronhowser.mods.critterworks.Critterworks
import net.minecraft.core.registries.Registries
import net.minecraft.tags.TagKey
import net.minecraft.world.level.biome.Biome

object ModBiomeTags {

	val HAS_SCOOCHWORM_APPLE: TagKey<Biome> = TagKey.create(
		Registries.BIOME,
		Critterworks.modResource("has_scoochworm_apple")
	)
}