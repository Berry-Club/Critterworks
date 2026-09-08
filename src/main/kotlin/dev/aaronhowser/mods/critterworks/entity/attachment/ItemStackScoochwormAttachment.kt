package dev.aaronhowser.mods.critterworks.entity.attachment

import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtOps
import net.minecraft.world.item.ItemStack
import kotlin.jvm.optionals.getOrNull

abstract class ItemStackScoochwormAttachment(
	itemStack: ItemStack
) : ScoochwormAttachment() {

	protected val itemStack: ItemStack = itemStack.copy()

	override val consumesItemStack: Boolean = true

	protected open fun synchronizeItemStack() {}

	protected fun saveItemStack(tag: CompoundTag) {
		synchronizeItemStack()

		val stackTag = ItemStack.OPTIONAL_CODEC.encodeStart(
			NbtOps.INSTANCE,
			itemStack
		)
			.result()
			.getOrNull()

		if (stackTag != null) {
			tag.put(ATTACHMENT_ITEM_TAG, stackTag)
		}
	}

	override fun remove(): ItemStack {
		synchronizeItemStack()
		return itemStack
	}

	companion object {
		private const val ATTACHMENT_ITEM_TAG = "AttachmentItem"
	}
}