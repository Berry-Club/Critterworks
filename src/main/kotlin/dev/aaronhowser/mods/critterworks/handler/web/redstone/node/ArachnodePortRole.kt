package dev.aaronhowser.mods.critterworks.handler.web.redstone.node

import com.mojang.serialization.Codec
import dev.aaronhowser.mods.aaron.serialization.AaronExtraStreamCodecs
import io.netty.buffer.ByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.util.StringRepresentable

enum class ArachnodePortRole(
	private val serializedName: String
) : StringRepresentable {
	INPUT("input"),
	OUTPUT("output");

	override fun getSerializedName(): String = serializedName

	companion object {
		val CODEC: Codec<ArachnodePortRole> =
			StringRepresentable.fromEnum { entries.toTypedArray() }

		val STREAM_CODEC: StreamCodec<ByteBuf, ArachnodePortRole> =
			AaronExtraStreamCodecs.enumStreamCodec(ArachnodePortRole::class.java)
	}
}