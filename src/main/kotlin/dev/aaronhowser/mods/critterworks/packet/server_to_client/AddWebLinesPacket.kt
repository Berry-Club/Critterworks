package dev.aaronhowser.mods.critterworks.packet.server_to_client

import dev.aaronhowser.mods.aaron.packet.AaronPacket
import dev.aaronhowser.mods.critterworks.Critterworks
import dev.aaronhowser.mods.critterworks.client.web.ClientWebState
import dev.aaronhowser.mods.critterworks.handler.web.WebLineType
import dev.aaronhowser.mods.critterworks.handler.web.common.line.WebLine
import dev.aaronhowser.mods.critterworks.handler.web.common.line.WebLineData
import dev.aaronhowser.mods.critterworks.handler.web.common.node.WebNode
import dev.aaronhowser.mods.critterworks.handler.web.redstone.node.Arachnode
import dev.aaronhowser.mods.critterworks.handler.web.redstone.node.ArachnodePort
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.neoforged.neoforge.network.handling.IPayloadContext

class AddWebLinesPacket(
	val webType: WebLineType,
	val arachnodes: List<Arachnode>,
	val nodes: List<WebNode>,
	val lines: List<WebLineData>
) : AaronPacket() {

	override fun type(): CustomPacketPayload.Type<out CustomPacketPayload?> {
		return TYPE
	}

	override fun handleOnClient(context: IPayloadContext) {
		ClientWebState.addLines(webType, arachnodes, nodes, lines)
	}

	companion object {
		val TYPE: CustomPacketPayload.Type<AddWebLinesPacket> =
			makeType(Critterworks.MOD_ID, "add_web_lines")

		val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, AddWebLinesPacket> = StreamCodec.composite(
			WebLineType.STREAM_CODEC, AddWebLinesPacket::webType,
			Arachnode.STREAM_CODEC.apply(ByteBufCodecs.list()), AddWebLinesPacket::arachnodes,
			WebNode.STREAM_CODEC.apply(ByteBufCodecs.list()), AddWebLinesPacket::nodes,
			WebLineData.STREAM_CODEC.apply(ByteBufCodecs.list()), AddWebLinesPacket::lines,
			::AddWebLinesPacket
		)

		fun fromLines(
			webType: WebLineType,
			lines: Collection<WebLine>,
			arachnodes: Collection<Arachnode> = emptyList()
		): AddWebLinesPacket {
			val nodes = lines
				.flatMap { line -> listOf(line.firstNode, line.secondNode) }
				.filter { node -> node !is ArachnodePort }
				.distinctBy(WebNode::uuid)

			return AddWebLinesPacket(
				webType,
				arachnodes.toList(),
				nodes,
				lines.map(WebLine::data)
			)
		}
	}
}