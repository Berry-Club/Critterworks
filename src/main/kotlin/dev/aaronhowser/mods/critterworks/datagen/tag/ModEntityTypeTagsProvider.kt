package dev.aaronhowser.mods.critterworks.datagen.tag

import dev.aaronhowser.mods.aaron.misc.AaronExtensions.add
import dev.aaronhowser.mods.critterworks.Critterworks
import dev.aaronhowser.mods.critterworks.registry.ModEntityTypes
import net.minecraft.core.HolderLookup
import net.minecraft.core.registries.Registries
import net.minecraft.data.PackOutput
import net.minecraft.data.tags.EntityTypeTagsProvider
import net.minecraft.tags.EntityTypeTags
import net.minecraft.tags.TagKey
import net.minecraft.world.entity.EntityType
import net.neoforged.neoforge.common.Tags
import net.neoforged.neoforge.common.data.ExistingFileHelper
import java.util.concurrent.CompletableFuture

class ModEntityTypeTagsProvider(
	output: PackOutput,
	provider: CompletableFuture<HolderLookup.Provider>,
	existingFileHelper: ExistingFileHelper
) : EntityTypeTagsProvider(output, provider, Critterworks.MOD_ID, existingFileHelper) {

	override fun addTags(provider: HolderLookup.Provider) {
		tag(EntityTypeTags.CAN_BREATHE_UNDER_WATER)
			.add(ModEntityTypes.PUNCHPRAWN)

		tag(EntityTypeTags.FALL_DAMAGE_IMMUNE)
			.add(
				ModEntityTypes.SCOOCHWORM,
				ModEntityTypes.SCOOCHWORM_PART
			)

		tag(Tags.EntityTypes.CAPTURING_NOT_SUPPORTED)
			.add(
				ModEntityTypes.SCOOCHWORM_PART
			)

		tag(Tags.EntityTypes.TELEPORTING_NOT_SUPPORTED)
			.add(
				ModEntityTypes.SCOOCHWORM_PART
			)

		tag(CAN_TARGET_FULLY_SHELLED_PLAYERS)
	}

	companion object {
		val CAN_TARGET_FULLY_SHELLED_PLAYERS: TagKey<EntityType<*>> =
			TagKey.create(
				Registries.ENTITY_TYPE,
				Critterworks.modResource("can_target_fully_shelled_players")
			)
	}

}