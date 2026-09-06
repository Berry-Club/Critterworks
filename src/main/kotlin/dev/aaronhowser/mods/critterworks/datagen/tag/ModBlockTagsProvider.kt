package dev.aaronhowser.mods.critterworks.datagen.tag

import dev.aaronhowser.mods.aaron.misc.AaronExtensions.add
import dev.aaronhowser.mods.critterworks.Critterworks
import dev.aaronhowser.mods.critterworks.registry.ModBlocks
import net.minecraft.core.HolderLookup
import net.minecraft.core.registries.Registries
import net.minecraft.data.PackOutput
import net.minecraft.tags.BlockTags
import net.minecraft.tags.TagKey
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.neoforged.neoforge.common.data.BlockTagsProvider
import net.neoforged.neoforge.common.data.ExistingFileHelper
import java.util.concurrent.CompletableFuture

class ModBlockTagsProvider(
	output: PackOutput,
	lookupProvider: CompletableFuture<HolderLookup.Provider>,
	existingFileHelper: ExistingFileHelper
) : BlockTagsProvider(output, lookupProvider, Critterworks.MOD_ID, existingFileHelper) {

	override fun addTags(provider: HolderLookup.Provider) {
		tag(PREVENTS_SCOOCHWORM_WANDERING)
			.addTag(BlockTags.ICE)
			.add(
				Blocks.SLIME_BLOCK,
				Blocks.HONEY_BLOCK
			)

		tag(SUPPORTS_SCOOCHWORM_TRAVEL)
			.add(
				ModBlocks.SCOOCHSTEM,
				ModBlocks.STEM_ENCASED_COMPARATOR,
				ModBlocks.SCOOCHSTEM_WOOD,
				ModBlocks.GREEN_SCOOCHSTEM,
				ModBlocks.BLUE_SCOOCHSTEM,
				ModBlocks.RED_SCOOCHSTEM,
				ModBlocks.YELLOW_SCOOCHSTEM,
				ModBlocks.MAGENTA_SCOOCHSTEM,
				ModBlocks.CYAN_SCOOCHSTEM,
				ModBlocks.SCOOCHWORM_DEPOT
			)

		tag(SCOOCHSTEM_REPLACEABLE)
			.addTag(BlockTags.MOSS_REPLACEABLE)

		tag(BlockTags.CLIMBABLE)
			.add(
				ModBlocks.DYEBERRY_VINES,
				ModBlocks.DYEBERRY_VINES_PLANT
			)

		tag(BlockTags.MINEABLE_WITH_AXE)
			.add(
				ModBlocks.SCOOCHSTEM,
				ModBlocks.STEM_ENCASED_COMPARATOR,
				ModBlocks.SCOOCHSTEM_WOOD,
				ModBlocks.GREEN_SCOOCHSTEM,
				ModBlocks.BLUE_SCOOCHSTEM,
				ModBlocks.RED_SCOOCHSTEM,
				ModBlocks.YELLOW_SCOOCHSTEM,
				ModBlocks.MAGENTA_SCOOCHSTEM,
				ModBlocks.CYAN_SCOOCHSTEM
			)

		tag(BlockTags.LOGS)
			.add(
				ModBlocks.SCOOCHSTEM,
				ModBlocks.SCOOCHSTEM_WOOD,
				ModBlocks.GREEN_SCOOCHSTEM,
				ModBlocks.BLUE_SCOOCHSTEM,
				ModBlocks.RED_SCOOCHSTEM,
				ModBlocks.YELLOW_SCOOCHSTEM,
				ModBlocks.MAGENTA_SCOOCHSTEM,
				ModBlocks.CYAN_SCOOCHSTEM
			)

		tag(BlockTags.LOGS_THAT_BURN)
			.add(
				ModBlocks.SCOOCHSTEM,
				ModBlocks.SCOOCHSTEM_WOOD,
				ModBlocks.GREEN_SCOOCHSTEM,
				ModBlocks.BLUE_SCOOCHSTEM,
				ModBlocks.RED_SCOOCHSTEM,
				ModBlocks.YELLOW_SCOOCHSTEM,
				ModBlocks.MAGENTA_SCOOCHSTEM,
				ModBlocks.CYAN_SCOOCHSTEM
			)
	}

	companion object {
		fun tk(name: String): TagKey<Block> =
			TagKey.create(Registries.BLOCK, Critterworks.modResource(name))

		val SUPPORTS_SCOOCHWORM_TRAVEL = tk("supports_scoochworm_travel")
		val PREVENTS_SCOOCHWORM_WANDERING = tk("prevents_scoochworm_wandering")
		val SCOOCHSTEM_REPLACEABLE = tk("scooch_replaceable")
	}

}