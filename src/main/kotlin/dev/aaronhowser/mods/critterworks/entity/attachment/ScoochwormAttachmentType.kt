package dev.aaronhowser.mods.critterworks.entity.attachment

import dev.aaronhowser.mods.critterworks.entity.attachment.data.SyncedAttachmentData
import io.netty.buffer.ByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.world.item.ItemStack

class ScoochwormAttachmentType<T : SyncedAttachmentData>(
	private val streamCodec: StreamCodec<ByteBuf, T>,
	private val matchesItem: (ItemStack) -> Boolean,
	private val createFromItem: (ItemStack) -> ScoochwormAttachment,
	private val createEmpty: () -> ScoochwormAttachment
) {
	fun create(itemStack: ItemStack): ScoochwormAttachment? {
		if (!matches(itemStack)) return null
		return createFromItem(itemStack)
	}

	fun matches(itemStack: ItemStack): Boolean {
		return matchesItem(itemStack)
	}

	fun createEmptyAttachment(): ScoochwormAttachment = createEmpty()

	@Suppress("UNCHECKED_CAST")
	fun createClientAttachment(data: SyncedAttachmentData): ScoochwormAttachment {
		val attachment = createFromItem(ItemStack.EMPTY)
		attachment.applySyncedData(data as T)
		return attachment
	}

	fun decode(buffer: ByteBuf): T {
		return streamCodec.decode(buffer)
	}

	@Suppress("UNCHECKED_CAST")
	fun encode(buffer: ByteBuf, data: SyncedAttachmentData) {
		streamCodec.encode(buffer, data as T)
	}
}
