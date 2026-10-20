package dev.aaronhowser.mods.critterworks.datagen.tag

import dev.aaronhowser.mods.aaron.misc.AaronExtensions.add
import dev.aaronhowser.mods.critterworks.registry.ModEntityTypes
import net.minecraft.core.HolderLookup
import net.minecraft.data.PackOutput
import net.minecraft.data.tags.EntityTypeTagsProvider
import net.minecraft.tags.EntityTypeTags
import java.util.concurrent.CompletableFuture

class ModEntityTypeTagsProvider(
	output: PackOutput,
	provider: CompletableFuture<HolderLookup.Provider>
) : EntityTypeTagsProvider(output, provider) {

	override fun addTags(provider: HolderLookup.Provider) {
		tag(EntityTypeTags.CAN_BREATHE_UNDER_WATER)
			.add(ModEntityTypes.PUNCHPRAWN)

		tag(EntityTypeTags.FALL_DAMAGE_IMMUNE)
			.add(
				ModEntityTypes.SCOOCHWORM,
				ModEntityTypes.SCOOCHWORM_PART
			)
	}

}