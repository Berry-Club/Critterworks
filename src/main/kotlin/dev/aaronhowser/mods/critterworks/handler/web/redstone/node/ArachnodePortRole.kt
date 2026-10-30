package dev.aaronhowser.mods.critterworks.handler.web.redstone.node

import com.mojang.serialization.Codec
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import io.netty.buffer.ByteBuf

enum class ArachnodePortRole(
	val serializedName: String
) {
	INPUT("input"),
	OUTPUT("output");

	companion object {
		val CODEC: Codec<ArachnodePortRole> = Codec.STRING.xmap(
			{ serializedName ->
				entries.firstOrNull { role -> role.serializedName == serializedName }
					?: error("Unknown arachnode port role: $serializedName")
			},
			ArachnodePortRole::serializedName
		)

		val STREAM_CODEC: StreamCodec<ByteBuf, ArachnodePortRole> = ByteBufCodecs.VAR_INT.map(
			{ ordinal -> entries.getOrElse(ordinal) { error("Unknown arachnode port role: $ordinal") } },
			ArachnodePortRole::ordinal
		)
	}
}