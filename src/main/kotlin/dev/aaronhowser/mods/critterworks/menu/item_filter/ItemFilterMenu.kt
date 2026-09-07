package dev.aaronhowser.mods.critterworks.menu.item_filter

import dev.aaronhowser.mods.aaron.menu.HeldItemMenu
import dev.aaronhowser.mods.aaron.menu.MenuWithButtons
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isItem
import dev.aaronhowser.mods.critterworks.item.ItemFilterItem
import dev.aaronhowser.mods.critterworks.item.component.ItemFilterComponent
import dev.aaronhowser.mods.critterworks.registry.ModItems
import dev.aaronhowser.mods.critterworks.registry.ModMenuTypes
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.items.ItemStackHandler
import net.neoforged.neoforge.items.SlotItemHandler

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

	private val filterItems = object : ItemStackHandler(ItemFilterComponent.CONTAINER_SIZE) {
		override fun onContentsChanged(slot: Int) {
			val stackThere = getStackInSlot(slot)
			val filterStack = getFilterStack()
			ItemFilterItem.setStack(filterStack, slot, stackThere)
		}
	}

	init {
		val filterStack = getFilterStack()
		val container = ItemFilterItem.getFilterItems(filterStack)
		for ((index, ghostStack) in container.withIndex()) {
			filterItems.setStackInSlot(index, ghostStack)
		}

		addSlots(159)
	}

	private fun getFilterStack(): ItemStack = playerInventory.player.getItemInHand(hand)

	private fun getFlagComponent(): ItemFilterComponent = ItemFilterItem.getFilterComponent(getFilterStack())

	fun isInverted(): Boolean = getFlagComponent().isInverted
	fun useTags(): Boolean = getFlagComponent().useTags
	fun ignoreDamage(): Boolean = getFlagComponent().ignoreDamage
	fun ignoreAllComponents(): Boolean = getFlagComponent().ignoreAllComponents

	override fun addContainerSlots() {
		for (index in 0 until ItemFilterComponent.CONTAINER_SIZE) {
			val x = 53 + (index % 4) * 18
			val y = 29 + (index / 4) * 18

			val slot = object : SlotItemHandler(filterItems, index, x, y) {
				override fun mayPlace(stack: ItemStack): Boolean = true
				override fun mayPickup(player: Player): Boolean = true
				override fun getMaxStackSize(): Int = 1
				override fun getMaxStackSize(stack: ItemStack): Int = 1
			}
			addSlot(slot)
		}
	}

	override fun isValidHeldItem(heldItem: ItemStack): Boolean {
		return heldItem.isItem(ModItems.ITEM_FILTER)
	}

	override fun handleButtonPressed(buttonId: Int, isShiftDown: Boolean) {
		val filterStack = getFilterStack()

		val toggledFlag = when (buttonId) {
			TOGGLE_INVERTED_BUTTON_ID -> ItemFilterComponent.Flag.INVERTED
			TOGGLE_USE_TAGS_BUTTON_ID -> ItemFilterComponent.Flag.USE_TAGS
			TOGGLE_IGNORE_DAMAGE_BUTTON_ID -> ItemFilterComponent.Flag.IGNORE_DAMAGE
			TOGGLE_IGNORE_ALL_COMPONENTS_BUTTON_ID -> ItemFilterComponent.Flag.IGNORE_ALL_COMPONENTS
			else -> return
		}

		val flags = getFlagComponent().flags.toMutableList()
		if (toggledFlag in flags) {
			flags.remove(toggledFlag)
		} else {
			flags.add(toggledFlag)
		}

		ItemFilterItem.setFlags(filterStack, flags)
	}

	companion object {
		const val TOGGLE_INVERTED_BUTTON_ID = 0
		const val TOGGLE_USE_TAGS_BUTTON_ID = 1
		const val TOGGLE_IGNORE_DAMAGE_BUTTON_ID = 2
		const val TOGGLE_IGNORE_ALL_COMPONENTS_BUTTON_ID = 3
	}
}