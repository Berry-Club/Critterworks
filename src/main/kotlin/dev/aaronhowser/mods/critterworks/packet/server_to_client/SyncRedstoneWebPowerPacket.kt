package dev.aaronhowser.mods.critterworks.packet.server_to_client

import dev.aaronhowser.mods.aaron.packet.AaronPacket
import dev.aaronhowser.mods.critterworks.Critterworks
import dev.aaronhowser.mods.critterworks.client.web.ClientWebLines
import dev.aaronhowser.mods.critterworks.handler.web.redstone.RedstoneWebPowerData
import io.netty.buffer.ByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.neoforged.neoforge.network.handling.IPayloadContext

class SyncRedstoneWebPowerPacket(
	val lines: List<RedstoneWebPowerData>
) : AaronPacket() {

	override fun type(): CustomPacketPayload.Type<SyncRedstoneWebPowerPacket> = TYPE

	override fun handleOnClient(context: IPayloadContext) {
		for (line in lines) {
			ClientWebLines.updateRedstonePower(line.lineUuid, line.power)
		}
	}

	companion object {
		val TYPE: CustomPacketPayload.Type<SyncRedstoneWebPowerPacket> =
			makeType(Critterworks.MOD_ID, "sync_redstone_web_power")

		val STREAM_CODEC: StreamCodec<ByteBuf, SyncRedstoneWebPowerPacket> = StreamCodec.composite(
			RedstoneWebPowerData.STREAM_CODEC.apply(ByteBufCodecs.list()), SyncRedstoneWebPowerPacket::lines,
			::SyncRedstoneWebPowerPacket
		)
	}
}