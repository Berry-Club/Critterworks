package dev.aaronhowser.mods.critterworks.entity.scoochworm.attachment.data

import dev.aaronhowser.mods.critterworks.registry.ModScoochwormAttachmentTypes
import io.netty.buffer.ByteBuf
import net.minecraft.network.codec.StreamCodec

data object NoAttachmentData : SyncedAttachmentData {
	override val typeId = ModScoochwormAttachmentTypes.NONE.id

	val STREAM_CODEC: StreamCodec<ByteBuf, NoAttachmentData> = StreamCodec.unit(this)
}