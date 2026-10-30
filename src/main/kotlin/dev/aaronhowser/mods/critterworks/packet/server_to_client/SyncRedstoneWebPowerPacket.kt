package dev.aaronhowser.mods.critterworks.packet.server_to_client

import dev.aaronhowser.mods.aaron.packet.AaronPacket
import dev.aaronhowser.mods.critterworks.Critterworks
import dev.aaronhowser.mods.critterworks.client.web.ClientWebState
import io.netty.buffer.ByteBuf
import net.minecraft.core.UUIDUtil
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.neoforged.neoforge.network.handling.IPayloadContext
import java.util.*

class SyncRedstoneWebPowerPacket(
	val lines: List<LinePower>
) : AaronPacket() {

	override fun type(): CustomPacketPayload.Type<SyncRedstoneWebPowerPacket> = TYPE

	override fun handleOnClient(context: IPayloadContext) {
		for (line in lines) {
			ClientWebState.updateRedstonePower(line.lineUuid, line.power)
		}
	}

	data class LinePower(
		val lineUuid: UUID,
		val power: Int
	) {
		companion object {
			val STREAM_CODEC: StreamCodec<ByteBuf, LinePower> = StreamCodec.composite(
				UUIDUtil.STREAM_CODEC, LinePower::lineUuid,
				ByteBufCodecs.VAR_INT, LinePower::power,
				::LinePower
			)
		}
	}

	companion object {
		val TYPE: CustomPacketPayload.Type<SyncRedstoneWebPowerPacket> =
			makeType(Critterworks.MOD_ID, "sync_redstone_web_power")

		val STREAM_CODEC: StreamCodec<ByteBuf, SyncRedstoneWebPowerPacket> = StreamCodec.composite(
			LinePower.STREAM_CODEC.apply(ByteBufCodecs.list()), SyncRedstoneWebPowerPacket::lines,
			::SyncRedstoneWebPowerPacket
		)
	}
}