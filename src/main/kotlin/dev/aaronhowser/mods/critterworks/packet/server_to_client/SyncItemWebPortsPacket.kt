package dev.aaronhowser.mods.critterworks.packet.server_to_client

import dev.aaronhowser.mods.aaron.packet.AaronPacket
import dev.aaronhowser.mods.critterworks.Critterworks
import dev.aaronhowser.mods.critterworks.client.web.ClientItemWebPorts
import dev.aaronhowser.mods.critterworks.handler.web.item.ItemWebPortData
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.neoforged.neoforge.network.handling.IPayloadContext

class SyncItemWebPortsPacket(
	val webPorts: List<ItemWebPortData>
) : AaronPacket() {

	override fun type(): CustomPacketPayload.Type<SyncItemWebPortsPacket> = TYPE

	override fun handleOnClient(context: IPayloadContext) {
		ClientItemWebPorts.update(webPorts)
	}

	companion object {
		val TYPE: CustomPacketPayload.Type<SyncItemWebPortsPacket> =
			makeType(Critterworks.MOD_ID, "sync_item_web_ports")

		val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, SyncItemWebPortsPacket> = StreamCodec.composite(
			ItemWebPortData.STREAM_CODEC.apply(ByteBufCodecs.list()), SyncItemWebPortsPacket::webPorts,
			::SyncItemWebPortsPacket
		)
	}
}