package dev.aaronhowser.mods.critterworks.menu.fluid_filter

import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isItem
import dev.aaronhowser.mods.critterworks.item.FluidFilterItem
import dev.aaronhowser.mods.critterworks.item.component.FluidFilterComponent
import dev.aaronhowser.mods.critterworks.item.component.FluidFilterEntry
import dev.aaronhowser.mods.critterworks.registry.ModDataComponents
import dev.aaronhowser.mods.critterworks.registry.ModItems
import net.minecraft.core.HolderLookup
import net.minecraft.world.SimpleContainer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.NonInteractiveResultSlot
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.fluids.FluidUtil
import java.util.function.Supplier

class FluidFilterSlot(
	private val filterStackGetter: Supplier<ItemStack>,
	private val registries: HolderLookup.Provider,
	private val filterIndex: Int,
	x: Int,
	y: Int
) : NonInteractiveResultSlot(SimpleContainer(0), 0, x, y) {

	private val filterComponent: FluidFilterComponent
		get() = FluidFilterItem.getFilterComponent(filterStackGetter.get())

	private val entry: FluidFilterEntry
		get() = filterComponent.entries.getOrNull(filterIndex) ?: FluidFilterEntry.Empty

	override fun mayPlace(stack: ItemStack): Boolean {
		return stack.isItem(ModItems.FLUID_FILTERFEEDER) || FluidUtil.getFluidContained(stack).isPresent
	}

	override fun mayPickup(player: Player): Boolean {
		val newEntries = filterComponent.entries.toMutableList()
		newEntries[filterIndex] = FluidFilterEntry.Empty
		setEntries(newEntries)

		return false
	}

	override fun safeInsert(stack: ItemStack): ItemStack {
		if (stack.isEmpty || entry !is FluidFilterEntry.Empty || !mayPlace(stack)) return stack

		val newEntries = filterComponent.entries.toMutableList()
		newEntries[filterIndex] = FluidFilterEntry.SpecificFluid(stack.copyWithCount(1), false)
		setEntries(newEntries)

		return stack
	}

	private fun setEntries(entries: List<FluidFilterEntry>) {
		filterStackGetter.get().set(
			ModDataComponents.FLUID_FILTER,
			FluidFilterComponent(entries, filterComponent.isBlacklist)
		)
	}

	override fun getItem(): ItemStack = entry.getDisplayStack(registries)
	override fun remove(amount: Int): ItemStack = ItemStack.EMPTY
	override fun set(stack: ItemStack) {}
	override fun isHighlightable(): Boolean = true
	override fun getSlotIndex(): Int = 0
}