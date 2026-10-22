package dev.aaronhowser.mods.critterworks.packet.client_to_server

import com.mojang.math.Constants
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isServerSide
import dev.aaronhowser.mods.aaron.packet.AaronPacket
import dev.aaronhowser.mods.critterworks.Critterworks
import dev.aaronhowser.mods.critterworks.item.SnailShellItem
import dev.aaronhowser.mods.critterworks.registry.ModAttachmentTypes
import io.netty.buffer.ByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3
import net.neoforged.neoforge.network.handling.IPayloadContext

class SnailShellMovedPacket private constructor(): AaronPacket() {

	override fun handleOnServer(context: IPayloadContext) {
		val player = context.player() as? ServerPlayer ?: return
		SnailShellItem.handleClientMovement(player)
	}

	override fun type(): CustomPacketPayload.Type<SnailShellMovedPacket> = TYPE

	companion object {
		val INSTANCE = SnailShellMovedPacket()

		val TYPE: CustomPacketPayload.Type<SnailShellMovedPacket> =
			makeType(Critterworks.MOD_ID, "snail_shell_moved")

		val STREAM_CODEC: StreamCodec<ByteBuf, SnailShellMovedPacket> =
			StreamCodec.unit(INSTANCE)

		// Local-only
		private var previousSnailShellPosition: Vec3? = null

		fun handleClientMovement(player: Player) {
			if (player.isServerSide) return

			val shellState = player.getData(ModAttachmentTypes.SNAIL_SHELL_STATE)
			if (!shellState.isShelled) {
				previousSnailShellPosition = null
				return
			}

			val currentPosition = player.position()
			val previousPosition = previousSnailShellPosition
			previousSnailShellPosition = currentPosition

			if (previousPosition == null) return
			if (currentPosition.distanceToSqr(previousPosition) <= Constants.EPSILON) return

			INSTANCE.messageServer()
		}

		fun clear() {
			previousSnailShellPosition = null
		}
	}

}