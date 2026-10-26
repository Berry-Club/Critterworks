package dev.aaronhowser.mods.critterworks.packet.client_to_server

import dev.aaronhowser.mods.aaron.packet.AaronPacket
import dev.aaronhowser.mods.critterworks.Critterworks
import dev.aaronhowser.mods.critterworks.item.CritterCageItem
import io.netty.buffer.ByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.neoforged.neoforge.network.handling.IPayloadContext

class UseCageOnEvilSnailPacket(
	val hand: InteractionHand
) : AaronPacket() {

	override fun handleOnServer(context: IPayloadContext) {
		val player = context.player() as? ServerPlayer ?: return
		CritterCageItem.tryClickSnail(player, hand)
	}

	override fun type(): CustomPacketPayload.Type<UseCageOnEvilSnailPacket> = TYPE

	companion object {
		val TYPE: CustomPacketPayload.Type<UseCageOnEvilSnailPacket> =
			makeType(Critterworks.MOD_ID, "use_cage_on_evil_snail")

		val STREAM_CODEC: StreamCodec<ByteBuf, UseCageOnEvilSnailPacket> = ByteBufCodecs.BOOL
			.map(
				{ isMainHand ->
					UseCageOnEvilSnailPacket(
						if (isMainHand) InteractionHand.MAIN_HAND else InteractionHand.OFF_HAND
					)
				},
				{ packet -> packet.hand == InteractionHand.MAIN_HAND }
			)
	}

}