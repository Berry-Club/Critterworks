package dev.aaronhowser.mods.critterworks.handler.web.common.line

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import io.netty.buffer.ByteBuf
import net.minecraft.core.UUIDUtil
import net.minecraft.network.codec.StreamCodec
import java.util.*

data class WebLineData(
	val uuid: UUID,
	val firstNodeUuid: UUID,
	val secondNodeUuid: UUID
) {
	companion object {
		val CODEC: Codec<WebLineData> = RecordCodecBuilder.create { instance ->
			instance.group(
				UUIDUtil.CODEC
					.fieldOf("uuid")
					.forGetter(WebLineData::uuid),
				UUIDUtil.CODEC
					.fieldOf("first_node_uuid")
					.forGetter(WebLineData::firstNodeUuid),
				UUIDUtil.CODEC
					.fieldOf("second_node_uuid")
					.forGetter(WebLineData::secondNodeUuid)
			).apply(instance, ::WebLineData)
		}

		val STREAM_CODEC: StreamCodec<ByteBuf, WebLineData> = StreamCodec.composite(
			UUIDUtil.STREAM_CODEC, WebLineData::uuid,
			UUIDUtil.STREAM_CODEC, WebLineData::firstNodeUuid,
			UUIDUtil.STREAM_CODEC, WebLineData::secondNodeUuid,
			::WebLineData
		)
	}
}