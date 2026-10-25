package dev.aaronhowser.mods.critterworks.packet.server_to_client

import dev.aaronhowser.mods.aaron.packet.AaronPacket
import dev.aaronhowser.mods.aaron.serialization.AaronExtraStreamCodecs
import dev.aaronhowser.mods.critterworks.Critterworks
import dev.aaronhowser.mods.critterworks.handler.snail.ClientEvilSnails
import io.netty.buffer.ByteBuf
import net.minecraft.core.UUIDUtil
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.world.phys.Vec3
import net.neoforged.neoforge.network.handling.IPayloadContext
import java.util.*

class SyncEvilSnailsPacket(
	val snails: List<SnailData>
) : AaronPacket() {

	override fun type(): CustomPacketPayload.Type<SyncEvilSnailsPacket> = TYPE

	override fun handleOnClient(context: IPayloadContext) {
		ClientEvilSnails.replaceWith(snails)
	}

	data class SnailData(
		val uuid: UUID,
		val position: Vec3,
		val yRot: Float
	) {

		companion object {
			val STREAM_CODEC: StreamCodec<ByteBuf, SnailData> = StreamCodec.composite(
				UUIDUtil.STREAM_CODEC, SnailData::uuid,
				AaronExtraStreamCodecs.VEC3, SnailData::position,
				ByteBufCodecs.FLOAT, SnailData::yRot,
				::SnailData
			)
		}
	}

	companion object {
		const val MAX_SNAILS = 256

		val TYPE: CustomPacketPayload.Type<SyncEvilSnailsPacket> =
			makeType(Critterworks.MOD_ID, "sync_evil_snails")

		val STREAM_CODEC: StreamCodec<ByteBuf, SyncEvilSnailsPacket> =
			SnailData.STREAM_CODEC
				.apply(ByteBufCodecs.list(MAX_SNAILS))
				.map(::SyncEvilSnailsPacket, SyncEvilSnailsPacket::snails)
	}

}