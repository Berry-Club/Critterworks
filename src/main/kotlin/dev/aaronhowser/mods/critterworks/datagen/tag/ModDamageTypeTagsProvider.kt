package dev.aaronhowser.mods.critterworks.datagen.tag

import dev.aaronhowser.mods.critterworks.Critterworks
import net.minecraft.core.HolderLookup
import net.minecraft.core.registries.Registries
import net.minecraft.data.PackOutput
import net.minecraft.data.tags.DamageTypeTagsProvider
import net.minecraft.tags.TagKey
import net.minecraft.world.damagesource.DamageType
import net.neoforged.neoforge.common.Tags
import net.neoforged.neoforge.common.data.ExistingFileHelper
import java.util.concurrent.CompletableFuture

class ModDamageTypeTagsProvider(
	output: PackOutput,
	lookupProvider: CompletableFuture<HolderLookup.Provider>,
	existingFileHelper: ExistingFileHelper
) : DamageTypeTagsProvider(output, lookupProvider, Critterworks.MOD_ID, existingFileHelper) {

	override fun addTags(provider: HolderLookup.Provider) {
		tag(TARDIGRADE_PROTECTS_FROM)
			.addTag(Tags.DamageTypes.IS_ENVIRONMENT)
	}

	companion object {
		fun tk(name: String): TagKey<DamageType> =
			TagKey.create(Registries.DAMAGE_TYPE, Critterworks.modResource(name))

		val TARDIGRADE_PROTECTS_FROM = tk("tardigrade_protects_from")
	}

}