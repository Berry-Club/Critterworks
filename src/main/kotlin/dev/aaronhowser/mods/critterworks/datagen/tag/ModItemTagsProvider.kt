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
			.add(ModItems.WEB_PATHFINDER)
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
				ModItems.WEB_PATHFINDER,
				ModItems.WEB_PORT
			)

		tag(REQUIRES_EXISTING_NODE)
			.add(
				ModItems.WEB_PATHFINDER,
				ModItems.WEB_PORT
			)
	}

	companion object {
		val DYEBERRIES = create("dyeberries")
		val SCOOCHWORM_LOOK_AT = create("scoochworm_look_at")
		val SCOOCHWORM_SADDLES = create("scoochworm_saddles")
		val WEB_LINE_INTERACTABLE = create("web_line_interactable")
		val SNAP_TO_NODE = create("snap_to_node")
		val REQUIRES_EXISTING_NODE = create("requires_existing_node")

		private fun create(id: String): TagKey<Item> = ItemTags.create(Critterworks.modResource(id))
	}
}