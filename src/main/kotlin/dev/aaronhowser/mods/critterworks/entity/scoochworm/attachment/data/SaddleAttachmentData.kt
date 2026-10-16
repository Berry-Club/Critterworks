package dev.aaronhowser.mods.critterworks.entity.scoochworm.attachment.data

import dev.aaronhowser.mods.critterworks.registry.ModScoochwormAttachmentTypes
import io.netty.buffer.ByteBuf
import net.minecraft.network.codec.StreamCodec

data object SaddleAttachmentData : SyncedAttachmentData {
	override val typeId = ModScoochwormAttachmentTypes.SADDLE.id

	val STREAM_CODEC: StreamCodec<ByteBuf, SaddleAttachmentData> = StreamCodec.unit(this)
}