package dev.aaronhowser.mods.critterworks.menu.stem_comparator

import dev.aaronhowser.mods.aaron.menu.MenuWithInventory
import dev.aaronhowser.mods.aaron.menu.components.FilteredSlot
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isItem
import dev.aaronhowser.mods.critterworks.registry.ModItems
import dev.aaronhowser.mods.critterworks.registry.ModMenuTypes
import net.minecraft.world.Container
import net.minecraft.world.SimpleContainer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player

class StemComparatorMenu(
	containerId: Int,
	playerInventory: Inventory,
	private val filterContainer: Container
) : MenuWithInventory(ModMenuTypes.STEM_COMPARATOR.get(), containerId, playerInventory) {

	constructor(containerId: Int, playerInventory: Inventory) :
		this(containerId, playerInventory, SimpleContainer(1))

	init {
		checkContainerSize(filterContainer, 1)
		addSlots(84)
	}

	override fun addContainerSlots() {
		val slot = FilteredSlot(filterContainer, 0, 80, 34) {
			it.isItem(ModItems.ITEM_FILTER)
		}

		addSlot(slot)
	}

	override fun stillValid(player: Player): Boolean = filterContainer.stillValid(player)

}