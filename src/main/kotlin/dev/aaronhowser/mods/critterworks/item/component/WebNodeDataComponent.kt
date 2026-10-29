package dev.aaronhowser.mods.critterworks.item.component

import com.mojang.serialization.Codec
import dev.aaronhowser.mods.critterworks.handler.web.item.node.WebNode
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec

data class WebNodeDataComponent(
	val node: WebNode
) {
	companion object {
		val CODEC: Codec<WebNodeDataComponent> =
			WebNode.CODEC.xmap(
				::WebNodeDataComponent,
				WebNodeDataComponent::node
			)

		val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, WebNodeDataComponent> =
			WebNode.STREAM_CODEC.map(
				::WebNodeDataComponent,
				WebNodeDataComponent::node
			)
	}
}