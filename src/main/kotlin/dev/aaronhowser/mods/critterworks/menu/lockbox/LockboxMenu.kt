package dev.aaronhowser.mods.critterworks.menu.lockbox

import dev.aaronhowser.mods.aaron.menu.MenuWithInventory
import dev.aaronhowser.mods.aaron.menu.components.ContainerSlot
import dev.aaronhowser.mods.critterworks.entity.scoochworm.attachment.builtin.LockboxContainer
import dev.aaronhowser.mods.critterworks.registry.ModMenuTypes
import net.minecraft.world.Container
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack

class LockboxMenu(
	containerId: Int,
	playerInventory: Inventory,
	private val lockboxContainer: Container
) : MenuWithInventory(ModMenuTypes.LOCKBOX.get(), containerId, playerInventory) {

	constructor(containerId: Int, playerInventory: Inventory) :
		this(containerId, playerInventory, LockboxContainer())

	init {
		checkContainerSize(lockboxContainer, LockboxContainer.CONTAINER_SIZE)
		lockboxContainer.startOpen(playerInventory.player)
		addSlots(100)
	}

	override fun addContainerSlots() {
		addSlot(
			ContainerSlot(
				lockboxContainer,
				LockboxContainer.FILTER_SLOT,
				152,
				7
			)
		)

		for (row in 0 until 3) {
			for (column in 0 until 9) {
				val slotIndex = column + row * 9
				val x = 8 + column * 18
				val y = 28 + row * 18

				addSlot(ContainerSlot(lockboxContainer, slotIndex, x, y))
			}
		}
	}

	override fun stillValid(player: Player): Boolean {
		return lockboxContainer.stillValid(player)
	}

	override fun moveItemStackTo(
		stack: ItemStack,
		startIndex: Int,
		endIndex: Int,
		reverseDirection: Boolean
	): Boolean {
		var movedItem = false

		if (stack.isStackable) {
			movedItem = moveIntoOccupiedSlots(stack, startIndex, endIndex, reverseDirection)
		}

		if (!stack.isEmpty) {
			movedItem = moveIntoEmptySlot(stack, startIndex, endIndex, reverseDirection) || movedItem
		}

		return movedItem
	}

	private fun moveIntoOccupiedSlots(
		stack: ItemStack,
		startIndex: Int,
		endIndex: Int,
		reverseDirection: Boolean
	): Boolean {
		var movedItem = false
		var slotIndex = getFirstSlotIndex(startIndex, endIndex, reverseDirection)

		while (!stack.isEmpty && slotIndex in startIndex until endIndex) {
			val slot = slots[slotIndex]
			val stackInSlot = slot.item

			if (ItemStack.isSameItemSameComponents(stack, stackInSlot)) {
				val previousCount = stack.count
				slot.safeInsert(stack)

				if (stack.count < previousCount) movedItem = true
			}

			slotIndex = getNextSlotIndex(slotIndex, reverseDirection)
		}

		return movedItem
	}

	private fun moveIntoEmptySlot(
		stack: ItemStack,
		startIndex: Int,
		endIndex: Int,
		reverseDirection: Boolean
	): Boolean {
		var slotIndex = getFirstSlotIndex(startIndex, endIndex, reverseDirection)

		while (slotIndex in startIndex until endIndex) {
			val slot = slots[slotIndex]
			if (!slot.hasItem()) {
				val previousCount = stack.count
				slot.safeInsert(stack)
				if (stack.count < previousCount) return true
			}

			slotIndex = getNextSlotIndex(slotIndex, reverseDirection)
		}

		return false
	}

	private fun getFirstSlotIndex(startIndex: Int, endIndex: Int, reverseDirection: Boolean): Int {
		return if (reverseDirection) endIndex - 1 else startIndex
	}

	private fun getNextSlotIndex(slotIndex: Int, reverseDirection: Boolean): Int {
		return if (reverseDirection) slotIndex - 1 else slotIndex + 1
	}

	override fun removed(player: Player) {
		super.removed(player)
		lockboxContainer.stopOpen(player)
	}

}