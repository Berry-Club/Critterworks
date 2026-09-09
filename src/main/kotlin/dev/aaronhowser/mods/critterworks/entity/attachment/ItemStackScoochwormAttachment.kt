package dev.aaronhowser.mods.critterworks.entity.attachment

import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtOps
import net.minecraft.world.item.ItemStack
import kotlin.jvm.optionals.getOrNull

abstract class ItemStackScoochwormAttachment(
	itemStack: ItemStack
) : ScoochwormAttachment() {

	protected var itemStack: ItemStack = itemStack.copy()

	override val consumesItemStack: Boolean = true

	protected open fun synchronizeItemStack() {}

	protected fun saveItemStack(tag: CompoundTag, registries: HolderLookup.Provider) {
		synchronizeItemStack()
		val registryOps = registries.createSerializationContext(NbtOps.INSTANCE)

		val stackTag = ItemStack.OPTIONAL_CODEC.encodeStart(
			registryOps,
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