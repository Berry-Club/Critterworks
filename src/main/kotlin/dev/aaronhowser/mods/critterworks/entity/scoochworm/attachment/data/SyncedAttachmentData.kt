package dev.aaronhowser.mods.critterworks.entity.scoochworm.attachment.data

import dev.aaronhowser.mods.critterworks.entity.scoochworm.attachment.ScoochwormAttachmentType
import dev.aaronhowser.mods.critterworks.registry.ModScoochwormAttachmentTypes
import io.netty.buffer.ByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.resources.ResourceLocation

interface SyncedAttachmentData {

	val typeId: ResourceLocation

	fun resolveType(): ScoochwormAttachmentType<*> {
		return ModScoochwormAttachmentTypes.REGISTRY.get(typeId)
			?: error("Unknown attachment data type: $typeId")
	}

	companion object {
		val STREAM_CODEC: StreamCodec<ByteBuf, SyncedAttachmentData> = object : StreamCodec<ByteBuf, SyncedAttachmentData> {
			override fun decode(buffer: ByteBuf): SyncedAttachmentData {
				val typeId = ResourceLocation.STREAM_CODEC.decode(buffer)
				val attachmentType = ModScoochwormAttachmentTypes.REGISTRY.get(typeId)
					?: error("Unknown attachment data type: $typeId")

				return attachmentType.decode(buffer)
			}

			override fun encode(buffer: ByteBuf, data: SyncedAttachmentData) {
				ResourceLocation.STREAM_CODEC.encode(buffer, data.typeId)
				data.resolveType().encode(buffer, data)
			}
		}
	}
}