package dev.aaronhowser.mods.critterworks.datagen.tag

import dev.aaronhowser.mods.aaron.misc.AaronExtensions.add
import dev.aaronhowser.mods.critterworks.Critterworks
import dev.aaronhowser.mods.critterworks.registry.ModBlocks
import dev.aaronhowser.mods.critterworks.registry.ModItems
import net.minecraft.core.HolderLookup
import net.minecraft.data.PackOutput
import net.minecraft.data.tags.ItemTagsProvider
import net.minecraft.tags.ItemTags
import net.minecraft.tags.TagKey
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Block
import net.neoforged.neoforge.common.Tags
import net.neoforged.neoforge.common.data.ExistingFileHelper
import java.util.concurrent.CompletableFuture

class ModItemTagsProvider(
	output: PackOutput,
	lookupProvider: CompletableFuture<HolderLookup.Provider>,
	blockTags: CompletableFuture<TagLookup<Block>>,
	existingFileHelper: ExistingFileHelper
) : ItemTagsProvider(output, lookupProvider, blockTags, Critterworks.MOD_ID, existingFileHelper) {

	override fun addTags(provider: HolderLookup.Provider) {
		tag(SPROUTSNAIL_FLOWERS)
			.add(
				Items.OAK_SAPLING,
				Items.SPRUCE_SAPLING,
				Items.BIRCH_SAPLING,
				Items.JUNGLE_SAPLING,
				Items.ACACIA_SAPLING,
				Items.CHERRY_SAPLING,
				Items.DARK_OAK_SAPLING,
				Items.MANGROVE_PROPAGULE,
				Items.FERN,
				Items.DANDELION,
				Items.POPPY,
				Items.BLUE_ORCHID,
				Items.ALLIUM,
				Items.AZURE_BLUET,
				Items.RED_TULIP,
				Items.ORANGE_TULIP,
				Items.WHITE_TULIP,
				Items.PINK_TULIP,
				Items.OXEYE_DAISY,
				Items.CORNFLOWER,
				Items.LILY_OF_THE_VALLEY,
				Items.WITHER_ROSE,
				Items.TORCHFLOWER,
				Items.RED_MUSHROOM,
				Items.BROWN_MUSHROOM,
				Items.DEAD_BUSH,
				Items.CACTUS,
				Items.BAMBOO,
				Items.CRIMSON_FUNGUS,
				Items.WARPED_FUNGUS,
				Items.CRIMSON_ROOTS,
				Items.WARPED_ROOTS,
				Items.AZALEA,
				Items.FLOWERING_AZALEA
			)

		tag(DYEBERRIES)
			.add(
				ModItems.RED_DYEBERRY,
				ModItems.BLUE_DYEBERRY,
				ModItems.GREEN_DYEBERRY,
				ModItems.CYAN_DYEBERRY,
				ModItems.YELLOW_DYEBERRY,
				ModItems.MAGENTA_DYEBERRY,
			)

		tag(SCOOCHWORM_LOOK_AT)
			.add(Items.MELON)
			.addTag(DYEBERRIES)

		tag(SCOOCHWORM_SADDLES)
			.add(Items.SADDLE)

		tag(WEB_LINE_INTERACTABLE)
			.add(ModItems.ARTIFICIAL_SPINNERETS)
			.add(ModItems.WEB_PORT)
			.addTag(Tags.Items.TOOLS_SHEAR)

		tag(ItemTags.LOGS)
			.add(
				ModBlocks.SCOOCHSTEM.asItem(),
				ModBlocks.SCOOCHSTEM_WOOD.asItem()
			)

		tag(ItemTags.LOGS_THAT_BURN)
			.add(
				ModBlocks.SCOOCHSTEM.asItem(),
				ModBlocks.SCOOCHSTEM_WOOD.asItem()
			)

		tag(SNAP_TO_NODE)
			.add(
				ModItems.ARTIFICIAL_SPINNERETS,
				ModItems.WEB_PORT
			)

		tag(REQUIRES_EXISTING_NODE)
			.add(
				ModItems.WEB_PORT
			)

		copy(ModBlockTagsProvider.SCOOCHSTEM_LOGS, SCOOCHSTEM_LOGS)

		tag(TARDIGRADE_BLACKLIST)
			.add(ModItems.TARDIGRADE)
	}

	companion object {
		private fun tk(id: String): TagKey<Item> =
			ItemTags.create(Critterworks.modResource(id))

		val DYEBERRIES = tk("dyeberries")
		val SPROUTSNAIL_FLOWERS = tk("sproutsnail_flowers")
		val SCOOCHWORM_LOOK_AT = tk("scoochworm_look_at")
		val SCOOCHWORM_SADDLES = tk("scoochworm_saddles")
		val WEB_LINE_INTERACTABLE = tk("web_line_interactable")
		val SNAP_TO_NODE = tk("snap_to_node")
		val REQUIRES_EXISTING_NODE = tk("requires_existing_node")
		val SCOOCHSTEM_LOGS = tk("scoochstem_logs")
		val TARDIGRADE_BLACKLIST = tk("tardigrade_blacklist")
	}
}