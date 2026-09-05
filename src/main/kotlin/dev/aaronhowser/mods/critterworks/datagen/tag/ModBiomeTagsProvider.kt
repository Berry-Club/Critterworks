package dev.aaronhowser.mods.critterworks.datagen.tag

import dev.aaronhowser.mods.critterworks.Critterworks
import dev.aaronhowser.mods.critterworks.registry.ModBiomeTags
import net.minecraft.core.HolderLookup
import net.minecraft.data.PackOutput
import net.minecraft.data.tags.BiomeTagsProvider
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
		tag(ModBiomeTags.HAS_SCOOCHWORM_APPLE)
			.add(Biomes.LUSH_CAVES)
	}
}