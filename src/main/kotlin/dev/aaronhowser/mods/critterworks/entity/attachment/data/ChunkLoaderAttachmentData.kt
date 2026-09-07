package dev.aaronhowser.mods.critterworks.entity.attachment.data

import dev.aaronhowser.mods.critterworks.registry.ModScoochwormAttachmentTypes
import io.netty.buffer.ByteBuf
import net.minecraft.core.UUIDUtil
import net.minecraft.network.codec.StreamCodec
import net.minecraft.resources.ResourceLocation
import java.util.*

data class ChunkLoaderAttachmentData(
	val uuid: UUID,
	val placerUuid: UUID
) : SyncedAttachmentData {
	override val typeId: ResourceLocation = ModScoochwormAttachmentTypes.CHUNK_LOADER.id

	companion object {
		val STREAM_CODEC: StreamCodec<ByteBuf, ChunkLoaderAttachmentData> =
			StreamCodec.composite(
				UUIDUtil.STREAM_CODEC, ChunkLoaderAttachmentData::uuid,
				UUIDUtil.STREAM_CODEC, ChunkLoaderAttachmentData::placerUuid,
				::ChunkLoaderAttachmentData
			)
	}
}