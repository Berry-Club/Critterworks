package dev.aaronhowser.mods.critterworks.entity.scoochworm.attachment.builtin

import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isItem
import dev.aaronhowser.mods.critterworks.item.ItemFilterItem
import dev.aaronhowser.mods.critterworks.registry.ModItems
import net.minecraft.world.SimpleContainer
import net.minecraft.world.item.ItemStack

open class LockboxContainer : SimpleContainer(CONTAINER_SIZE) {

	override fun canPlaceItem(slot: Int, stack: ItemStack): Boolean {
		if (slot == FILTER_SLOT) {
			return stack.isItem(ModItems.ITEM_FILTERFEEDER)
		}

		if (slot !in 0 until STORAGE_SLOT_COUNT) return false

		val filterStack = getItem(FILTER_SLOT)
		return filterStack.isEmpty || ItemFilterItem.passesFilter(filterStack, stack)
	}

	companion object {
		const val STORAGE_SLOT_COUNT = 27
		const val FILTER_SLOT = STORAGE_SLOT_COUNT
		const val CONTAINER_SIZE = STORAGE_SLOT_COUNT + 1
	}

}