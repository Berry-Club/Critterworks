package dev.aaronhowser.mods.critterworks.packet.server_to_client

import dev.aaronhowser.mods.aaron.packet.AaronPacket
import dev.aaronhowser.mods.aaron.serialization.AaronExtraStreamCodecs
import dev.aaronhowser.mods.critterworks.Critterworks
import dev.aaronhowser.mods.critterworks.client.web.WebPathIndicatorRenderer
import io.netty.buffer.ByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.world.phys.Vec3
import net.neoforged.neoforge.network.handling.IPayloadContext

class ShowWebPathPacket(val positions: List<Vec3>) : AaronPacket() {

	override fun type(): CustomPacketPayload.Type<ShowWebPathPacket> = TYPE

	override fun handleOnClient(context: IPayloadContext) {
		WebPathIndicatorRenderer.show(positions, DURATION_TICKS)
	}

	companion object {
		private const val DURATION_TICKS = 5 * 20

		val TYPE: CustomPacketPayload.Type<ShowWebPathPacket> =
			makeType(Critterworks.MOD_ID, "show_web_path")

		val STREAM_CODEC: StreamCodec<ByteBuf, ShowWebPathPacket> =
			AaronExtraStreamCodecs.VEC3
				.apply(ByteBufCodecs.list())
				.map(::ShowWebPathPacket, ShowWebPathPacket::positions)
	}
}