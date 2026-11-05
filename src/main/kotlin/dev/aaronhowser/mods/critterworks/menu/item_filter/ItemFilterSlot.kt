package dev.aaronhowser.mods.critterworks.menu.item_filter

import dev.aaronhowser.mods.critterworks.item.ItemFilterItem
import dev.aaronhowser.mods.critterworks.item.component.ItemFilterComponent
import dev.aaronhowser.mods.critterworks.item.component.ItemFilterEntry
import dev.aaronhowser.mods.critterworks.registry.ModDataComponents
import net.minecraft.core.HolderLookup
import net.minecraft.world.SimpleContainer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.NonInteractiveResultSlot
import net.minecraft.world.item.ItemStack
import java.util.function.Supplier

class ItemFilterSlot(
	private val filterStackGetter: Supplier<ItemStack>,
	private val registries: HolderLookup.Provider,
	private val filterIndex: Int,
	x: Int,
	y: Int
) : NonInteractiveResultSlot(SimpleContainer(0), 0, x, y) {

	private val filterComponent: ItemFilterComponent
		get() = ItemFilterItem.getFilterComponent(filterStackGetter.get())

	private val entry: ItemFilterEntry
		get() = filterComponent.entries.getOrNull(filterIndex) ?: ItemFilterEntry.Empty

	override fun mayPickup(player: Player): Boolean {
		val newEntries = filterComponent.entries.toMutableList()
		newEntries[filterIndex] = ItemFilterEntry.Empty
		setEntries(newEntries)

		return false
	}

	override fun safeInsert(stack: ItemStack): ItemStack {
		if (stack.isEmpty || entry !is ItemFilterEntry.Empty) return stack

		val newEntries = filterComponent.entries.toMutableList()
		newEntries[filterIndex] = ItemFilterEntry.SpecificItem(
			stack.copyWithCount(1),
			ItemFilterEntry.ComponentMatch.IGNORE_ALL
		)
		setEntries(newEntries)

		return stack
	}

	private fun setEntries(entries: List<ItemFilterEntry>) {
		filterStackGetter.get().set(
			ModDataComponents.ITEM_FILTER,
			ItemFilterComponent(entries, filterComponent.isBlacklist)
		)
	}

	override fun getItem(): ItemStack = entry.getDisplayStack(registries)
	override fun remove(amount: Int): ItemStack = ItemStack.EMPTY
	override fun set(stack: ItemStack) {}
	override fun isHighlightable(): Boolean = true
	override fun getSlotIndex(): Int = 0
}