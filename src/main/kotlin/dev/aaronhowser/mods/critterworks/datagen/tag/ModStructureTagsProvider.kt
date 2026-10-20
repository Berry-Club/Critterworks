package dev.aaronhowser.mods.critterworks.datagen.tag

import dev.aaronhowser.mods.critterworks.Critterworks
import net.minecraft.core.HolderLookup
import net.minecraft.core.registries.Registries
import net.minecraft.data.PackOutput
import net.minecraft.data.tags.StructureTagsProvider
import net.minecraft.tags.TagKey
import net.minecraft.world.level.levelgen.structure.BuiltinStructures
import net.minecraft.world.level.levelgen.structure.Structure
import net.neoforged.neoforge.common.data.ExistingFileHelper
import java.util.concurrent.CompletableFuture

class ModStructureTagsProvider(
	output: PackOutput,
	lookupProvider: CompletableFuture<HolderLookup.Provider>,
	existingFileHelper: ExistingFileHelper
) : StructureTagsProvider(
	output,
	lookupProvider,
	Critterworks.MOD_ID,
	existingFileHelper
) {

	override fun addTags(provider: HolderLookup.Provider) {
		tag(HAS_PUNCHPRAWN)
			.add(BuiltinStructures.OCEAN_MONUMENT)
	}

	companion object {
		val HAS_PUNCHPRAWN: TagKey<Structure> = TagKey.create(
			Registries.STRUCTURE,
			Critterworks.modResource("has_punchprawn")
		)
	}

}