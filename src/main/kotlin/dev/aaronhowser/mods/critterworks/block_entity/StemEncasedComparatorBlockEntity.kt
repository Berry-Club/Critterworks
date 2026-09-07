package dev.aaronhowser.mods.critterworks.block_entity

import dev.aaronhowser.mods.aaron.block_entity.SyncingBlockEntity
import dev.aaronhowser.mods.aaron.container.ContainerContainer
import dev.aaronhowser.mods.aaron.container.ImprovedSimpleContainer
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isItem
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.loadItems
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.saveItems
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.toComponent
import dev.aaronhowser.mods.critterworks.menu.stem_comparator.StemComparatorMenu
import dev.aaronhowser.mods.critterworks.registry.ModBlockEntityTypes
import dev.aaronhowser.mods.critterworks.registry.ModItems
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.world.Container
import net.minecraft.world.MenuProvider
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.state.BlockState

class StemEncasedComparatorBlockEntity(pos: BlockPos, state: BlockState) :
	SyncingBlockEntity(ModBlockEntityTypes.STEM_ENCASED_COMPARATOR.get(), pos, state), MenuProvider, ContainerContainer {

	val filterContainer = object : ImprovedSimpleContainer(this, 1) {
		override fun canPlaceItem(slot: Int, stack: ItemStack): Boolean =
			stack.isItem(ModItems.ITEM_FILTER.get())
	}

	override fun getContainers(): List<Container> = listOf(filterContainer)

	override fun getDisplayName(): Component = blockState.block.name

	override fun createMenu(containerId: Int, playerInventory: Inventory, player: Player): AbstractContainerMenu {
		return StemComparatorMenu(containerId, playerInventory, filterContainer)
	}

	override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
		super.saveAdditional(tag, registries)
		tag.saveItems(filterContainer, registries)
	}

	override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
		super.loadAdditional(tag, registries)
		tag.loadItems(filterContainer, registries)
	}

}