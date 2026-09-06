package dev.aaronhowser.mods.critterworks.datagen.loot

import dev.aaronhowser.mods.aaron.datagen.AaronLootTableDsl
import dev.aaronhowser.mods.critterworks.registry.ModBlocks
import dev.aaronhowser.mods.critterworks.registry.ModDataComponents
import net.minecraft.core.HolderLookup
import net.minecraft.core.registries.Registries
import net.minecraft.data.loot.BlockLootSubProvider
import net.minecraft.world.flag.FeatureFlags
import net.minecraft.world.item.Items
import net.minecraft.world.item.enchantment.Enchantments
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.storage.loot.entries.LootItem
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator
import net.neoforged.neoforge.registries.DeferredHolder

class ModBlockLootTablesSubProvider(
	private val provider: HolderLookup.Provider
) : BlockLootSubProvider(setOf(), FeatureFlags.REGISTRY.allFlags(), provider) {

	override fun generate() {
		critterCage()
		dropSelf(ModBlocks.SCOOCHSTEM.get())
		dropSelf(ModBlocks.STEM_ENCASED_COMPARATOR.get())
		dropSelf(ModBlocks.SCOOCHSTEM_WOOD.get())
		dropSelf(ModBlocks.HOPPING_SPIDER_NEST.get())
		dropSelf(ModBlocks.SCOOCHWORM_DEPOT.get())
		appleSlice()

		dropSelf(ModBlocks.GREEN_SCOOCHSTEM.get())
		dropSelf(ModBlocks.BLUE_SCOOCHSTEM.get())
		dropSelf(ModBlocks.RED_SCOOCHSTEM.get())
		dropSelf(ModBlocks.YELLOW_SCOOCHSTEM.get())
		dropSelf(ModBlocks.MAGENTA_SCOOCHSTEM.get())
		dropSelf(ModBlocks.CYAN_SCOOCHSTEM.get())

		for (block in ModBlocks.BLOCK_REGISTRY.entries) {
			if (block.id.path.endsWith("dyeberry_vines") || block.id.path.endsWith("dyeberry_vines_plant")) {
				add(block.get(), noDrop())
			}
		}
	}

	private fun critterCage() {
		add(
			ModBlocks.CRITTER_CAGE.get(),
			AaronLootTableDsl.table {
				pool {
					rolls(1f)
					item(ModBlocks.CRITTER_CAGE.get()) {
						apply(
							CopyComponentsFunction.copyComponents(
								CopyComponentsFunction.Source.BLOCK_ENTITY
							)
								.include(ModDataComponents.ENTITY_DATA.get())
						)
					}
				}
			}
		)
	}

	private fun appleSlice() {
		val fortune = provider
			.lookupOrThrow(Registries.ENCHANTMENT)
			.getOrThrow(Enchantments.FORTUNE)
		val appleDrops = LootItem.lootTableItem(Items.APPLE)
			.apply(SetItemCountFunction.setCount(UniformGenerator.between(1f, 3f)))
			.apply(ApplyBonusCount.addUniformBonusCount(fortune))

		add(
			ModBlocks.APPLE_SLICE.get(),
			createSilkTouchDispatchTable(ModBlocks.APPLE_SLICE.get(), appleDrops)
		)
	}

	override fun getKnownBlocks(): Iterable<Block> {
		return ModBlocks.BLOCK_REGISTRY.entries.map(DeferredHolder<Block, out Block>::get)
	}
}