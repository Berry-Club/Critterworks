package dev.aaronhowser.mods.critterworks.packet.client_to_server

import dev.aaronhowser.mods.aaron.packet.AaronPacket
import dev.aaronhowser.mods.aaron.serialization.AaronExtraStreamCodecs
import dev.aaronhowser.mods.critterworks.Critterworks
import dev.aaronhowser.mods.critterworks.handler.web.common.interaction.WebInteractionHandler
import dev.aaronhowser.mods.critterworks.handler.web.WebLineType
import io.netty.buffer.ByteBuf
import net.minecraft.core.UUIDUtil
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.phys.Vec3
import net.neoforged.neoforge.network.handling.IPayloadContext
import java.util.*

class WebLineInteractPacket(
	val webType: WebLineType,
	val targetUuid: UUID,
	val targetsNode: Boolean,
	val position: Vec3,
	val hand: InteractionHand
) : AaronPacket() {

	override fun handleOnServer(context: IPayloadContext) {
		val player = context.player() as? ServerPlayer ?: return
		WebInteractionHandler.interactFromC2S(player, webType, targetUuid, targetsNode, position, hand)
	}

	override fun type(): CustomPacketPayload.Type<WebLineInteractPacket> = TYPE

	companion object {
		val TYPE: CustomPacketPayload.Type<WebLineInteractPacket> =
			makeType(Critterworks.MOD_ID, "web_line_interact")

		val STREAM_CODEC: StreamCodec<ByteBuf, WebLineInteractPacket> = StreamCodec.composite(
			WebLineType.STREAM_CODEC, WebLineInteractPacket::webType,
			UUIDUtil.STREAM_CODEC, WebLineInteractPacket::targetUuid,
			ByteBufCodecs.BOOL, WebLineInteractPacket::targetsNode,
			AaronExtraStreamCodecs.VEC3, WebLineInteractPacket::position,
			ByteBufCodecs.BOOL.map(
				{ isMainHand -> if (isMainHand) InteractionHand.MAIN_HAND else InteractionHand.OFF_HAND },
				{ hand -> hand == InteractionHand.MAIN_HAND }
			), WebLineInteractPacket::hand,
			::WebLineInteractPacket
		)
	}
}