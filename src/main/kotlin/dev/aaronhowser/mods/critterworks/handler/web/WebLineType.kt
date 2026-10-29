package dev.aaronhowser.mods.critterworks.handler.web

import io.netty.buffer.ByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec

enum class WebLineType {
	ITEM,
	REDSTONE;

	companion object {
		val STREAM_CODEC: StreamCodec<ByteBuf, WebLineType> =
			ByteBufCodecs.idMapper(
				WebLineType::fromOrdinal,
				WebLineType::ordinal
			)

		private fun fromOrdinal(ordinal: Int): WebLineType {
			return entries.getOrElse(ordinal) { ITEM }
		}
	}
}