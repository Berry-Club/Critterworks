package dev.aaronhowser.mods.critterworks.handler.web.redstone

import io.netty.buffer.ByteBuf
import net.minecraft.core.UUIDUtil
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import java.util.*

data class RedstoneWebPowerData(
	val lineUuid: UUID,
	val power: Int
) {
	companion object {
		val STREAM_CODEC: StreamCodec<ByteBuf, RedstoneWebPowerData> = StreamCodec.composite(
			UUIDUtil.STREAM_CODEC, RedstoneWebPowerData::lineUuid,
			ByteBufCodecs.VAR_INT, RedstoneWebPowerData::power,
			::RedstoneWebPowerData
		)
	}
}