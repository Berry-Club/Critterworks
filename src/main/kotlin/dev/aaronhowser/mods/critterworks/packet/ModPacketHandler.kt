package dev.aaronhowser.mods.critterworks.packet

import dev.aaronhowser.mods.aaron.packet.AaronPacketRegistrar
import dev.aaronhowser.mods.critterworks.packet.client_to_server.WebLineInteractPacket
import dev.aaronhowser.mods.critterworks.packet.server_to_client.AddWebLinesPacket
import dev.aaronhowser.mods.critterworks.packet.server_to_client.RemoveWebLinePacket
import dev.aaronhowser.mods.critterworks.packet.server_to_client.ShowWebPathPacket
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent

object ModPacketHandler : AaronPacketRegistrar {

	fun registerPayloads(event: RegisterPayloadHandlersEvent) {
		val registrar = event.registrar("1")

		toClient(registrar, AddWebLinesPacket.TYPE, AddWebLinesPacket.STREAM_CODEC)
		toClient(registrar, RemoveWebLinePacket.TYPE, RemoveWebLinePacket.STREAM_CODEC)
		toClient(registrar, ShowWebPathPacket.TYPE, ShowWebPathPacket.STREAM_CODEC)
		toServer(registrar, WebLineInteractPacket.TYPE, WebLineInteractPacket.STREAM_CODEC)
	}
}
