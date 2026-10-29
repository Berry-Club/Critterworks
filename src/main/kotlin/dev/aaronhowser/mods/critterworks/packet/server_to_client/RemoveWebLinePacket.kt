package dev.aaronhowser.mods.critterworks.packet.server_to_client

import dev.aaronhowser.mods.aaron.packet.AaronPacket
import dev.aaronhowser.mods.critterworks.Critterworks
import dev.aaronhowser.mods.critterworks.handler.web.common.line.ClientWebLines
import io.netty.buffer.ByteBuf
import net.minecraft.core.UUIDUtil
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.neoforged.neoforge.network.handling.IPayloadContext
import java.util.*

class RemoveWebLinePacket(
	val uuid: UUID
) : AaronPacket() {

	override fun type(): CustomPacketPayload.Type<out CustomPacketPayload?> {
		return TYPE
	}

	override fun handleOnClient(context: IPayloadContext) {
		ClientWebLines.removeLine(uuid)
	}

	companion object {
		val TYPE: CustomPacketPayload.Type<RemoveWebLinePacket> =
			makeType(Critterworks.MOD_ID, "remove_web_line")

		val STREAM_CODEC: StreamCodec<ByteBuf, RemoveWebLinePacket> = StreamCodec.composite(
			UUIDUtil.STREAM_CODEC, RemoveWebLinePacket::uuid,
			::RemoveWebLinePacket
		)
	}
}