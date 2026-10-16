package dev.aaronhowser.mods.critterworks.entity.scoochworm.attachment.data

import dev.aaronhowser.mods.critterworks.registry.ModScoochwormAttachmentTypes
import io.netty.buffer.ByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec

data class LockboxAttachmentData(
	val isOpen: Boolean
) : SyncedAttachmentData {
	override val typeId = ModScoochwormAttachmentTypes.LOCKBOX.id

	companion object {
		val STREAM_CODEC: StreamCodec<ByteBuf, LockboxAttachmentData> = ByteBufCodecs.BOOL.map(
			::LockboxAttachmentData,
			LockboxAttachmentData::isOpen
		)
	}
}