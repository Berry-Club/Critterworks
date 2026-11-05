package dev.aaronhowser.mods.critterworks.menu.item_filter

import dev.aaronhowser.mods.aaron.menu.HeldItemMenu
import dev.aaronhowser.mods.aaron.menu.MenuWithButtons
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isItem
import dev.aaronhowser.mods.critterworks.item.ItemFilterItem
import dev.aaronhowser.mods.critterworks.item.component.ItemFilterComponent
import dev.aaronhowser.mods.critterworks.item.component.ItemFilterEntry
import dev.aaronhowser.mods.critterworks.registry.ModDataComponents
import dev.aaronhowser.mods.critterworks.registry.ModItems
import dev.aaronhowser.mods.critterworks.registry.ModMenuTypes
import net.minecraft.core.NonNullList
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack

class ItemFilterMenu(
	containerId: Int,
	playerInventory: Inventory,
	val hand: InteractionHand
) : HeldItemMenu(ModMenuTypes.ITEM_FILTER.get(), containerId, playerInventory, hand), MenuWithButtons {

	constructor(
		containerId: Int,
		playerInventory: Inventory,
		data: RegistryFriendlyByteBuf
	) : this(containerId, playerInventory, data.readEnum(InteractionHand::class.java))

	init {
		addSlots(59)
	}

	private fun getFilterStack(): ItemStack = playerInventory.player.getItemInHand(hand)
	private fun getFilterComponent(): ItemFilterComponent = ItemFilterItem.getFilterComponent(getFilterStack())

	fun getEntries(): NonNullList<ItemFilterEntry> = getFilterComponent().entries
	fun isBlacklist(): Boolean = getFilterComponent().isBlacklist

	override fun addContainerSlots() {
		val registries = playerInventory.player.registryAccess()

		for (index in 0 until ItemFilterComponent.CONTAINER_SIZE) {
			val slot = ItemFilterSlot(::getFilterStack, registries, index, 8 + index * 18, 26)
			addSlot(slot)
		}
	}

	override fun quickMoveStack(player: Player, clickedSlotIndex: Int): ItemStack = ItemStack.EMPTY

	override fun isValidHeldItem(heldItem: ItemStack): Boolean {
		return heldItem.isItem(ModItems.ITEM_FILTERFEEDER)
	}

	override fun handleButtonPressed(buttonId: Int, isShiftDown: Boolean) {
		when (buttonId) {
			TOGGLE_BLACKLIST_BUTTON_ID -> setBlacklist(!isBlacklist())
			in 1..9 -> toggleEntryType(buttonId - 1)
			in 10..18 -> cycleEntryOption(buttonId - 10)
		}
	}

	private fun setBlacklist(value: Boolean) {
		val component = getFilterComponent()
		if (value == component.isBlacklist) return

		getFilterStack().set(ModDataComponents.ITEM_FILTER, component.copy(isBlacklist = value))
	}

	private fun toggleEntryType(slotIndex: Int) {
		val entries = getEntries()
		val entry = entries.getOrNull(slotIndex) ?: return
		val newEntry = when (entry) {
			is ItemFilterEntry.Tag -> entry.asSpecificItem()
			is ItemFilterEntry.SpecificItem -> {
				val tag = entry.stack.tags.findFirst().orElse(null) ?: return
				ItemFilterEntry.Tag(tag, entry.stack.copy())
			}
			ItemFilterEntry.Empty -> return
		}

		setEntry(slotIndex, newEntry)
	}

	private fun cycleEntryOption(slotIndex: Int) {
		val entry = getEntries().getOrNull(slotIndex) ?: return
		val newEntry = when (entry) {
			is ItemFilterEntry.Tag -> entry.copy(tagKey = entry.getNextTag())
			is ItemFilterEntry.SpecificItem -> entry.withNextComponentMatch()
			ItemFilterEntry.Empty -> return
		}

		setEntry(slotIndex, newEntry)
	}

	private fun setEntry(slotIndex: Int, entry: ItemFilterEntry) {
		val component = getFilterComponent()
		val entries = component.entries.toMutableList()
		entries[slotIndex] = entry

		getFilterStack().set(
			ModDataComponents.ITEM_FILTER,
			ItemFilterComponent(entries, component.isBlacklist)
		)
	}

	companion object {
		const val TOGGLE_BLACKLIST_BUTTON_ID = 0

		fun getTypeButtonId(slotIndex: Int): Int = slotIndex + 1
		fun getOptionButtonId(slotIndex: Int): Int = slotIndex + 10
	}
}