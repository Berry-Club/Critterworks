package dev.aaronhowser.mods.critterworks.datagen.tag

import dev.aaronhowser.mods.critterworks.Critterworks
import net.minecraft.core.HolderLookup
import net.minecraft.core.registries.Registries
import net.minecraft.data.PackOutput
import net.minecraft.data.tags.BiomeTagsProvider
import net.minecraft.tags.BiomeTags
import net.minecraft.tags.TagKey
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.biome.Biomes
import net.neoforged.neoforge.common.data.ExistingFileHelper
import java.util.concurrent.CompletableFuture

class ModBiomeTagsProvider(
	output: PackOutput,
	lookupProvider: CompletableFuture<HolderLookup.Provider>,
	existingFileHelper: ExistingFileHelper
) : BiomeTagsProvider(
	output,
	lookupProvider,
	Critterworks.MOD_ID,
	existingFileHelper
) {

	override fun addTags(provider: HolderLookup.Provider) {
		tag(HAS_SCOOCHWORM_APPLE)
			.add(Biomes.LUSH_CAVES)

		tag(HAS_HOPPING_SPIDER_NEST)
			.addTag(BiomeTags.IS_OVERWORLD)
	}

	companion object {
		fun key(name: String): TagKey<Biome> =
			TagKey.create(Registries.BIOME, Critterworks.modResource(name))

		val HAS_SCOOCHWORM_APPLE: TagKey<Biome> = key("has_scoochworm_apple")
		val HAS_HOPPING_SPIDER_NEST: TagKey<Biome> = key("has_hopping_spider_nest")
	}

}