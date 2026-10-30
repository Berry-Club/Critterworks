package dev.aaronhowser.mods.critterworks.handler.web

import com.mojang.serialization.Codec
import dev.aaronhowser.mods.aaron.serialization.AaronExtraStreamCodecs
import io.netty.buffer.ByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.util.StringRepresentable

enum class WebLineType(
	private val serializedName: String
) : StringRepresentable {
	ITEM("item"),
	REDSTONE("redstone");

	override fun getSerializedName(): String = serializedName

	companion object {
		val CODEC: Codec<WebLineType> =
			StringRepresentable.fromEnum { entries.toTypedArray() }

		val STREAM_CODEC: StreamCodec<ByteBuf, WebLineType> =
			AaronExtraStreamCodecs.enumStreamCodec(WebLineType::class.java)
	}
}