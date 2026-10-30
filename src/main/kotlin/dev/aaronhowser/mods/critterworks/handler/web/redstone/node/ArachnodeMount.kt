package dev.aaronhowser.mods.critterworks.handler.web.redstone.node

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import dev.aaronhowser.mods.aaron.serialization.AaronExtraStreamCodecs
import dev.aaronhowser.mods.critterworks.handler.web.common.node.WebAnchorSupport
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.phys.Vec3

sealed class ArachnodeMount {

	data object Line : ArachnodeMount()

	data class Surface(
		val blockPosition: BlockPos,
		val face: Direction,
		val contactPosition: Vec3
	) : ArachnodeMount() {

		fun getSurfaceNormal(): Vec3 {
			return Vec3.atLowerCornerOf(face.normal)
		}

		fun isValid(level: ServerLevel): Boolean {
			return WebAnchorSupport.hasSupport(level, blockPosition, face, contactPosition)
		}

		companion object {
			val CODEC: MapCodec<Surface> = RecordCodecBuilder.mapCodec { instance ->
				instance.group(
					BlockPos.CODEC
						.fieldOf("block_position")
						.forGetter(Surface::blockPosition),
					Direction.CODEC
						.fieldOf("face")
						.forGetter(Surface::face),
					Vec3.CODEC
						.fieldOf("contact_position")
						.forGetter(Surface::contactPosition)
				).apply(instance, ::Surface)
			}
		}
	}

	companion object {
		private const val LINE_TYPE = "line"
		private const val SURFACE_TYPE = "surface"
		private const val LINE_TYPE_ID = 0
		private const val SURFACE_TYPE_ID = 1

		val CODEC: Codec<ArachnodeMount> = Codec.STRING.dispatch(
			"type",
			{ mount ->
				when (mount) {
					Line -> LINE_TYPE
					is Surface -> SURFACE_TYPE
				}
			},
			{ type ->
				when (type) {
					LINE_TYPE -> MapCodec.unit(Line)
					SURFACE_TYPE -> Surface.CODEC
					else -> error("Unknown arachnode mount type: $type")
				}
			}
		)

		val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, ArachnodeMount> =
			object : StreamCodec<RegistryFriendlyByteBuf, ArachnodeMount> {
				override fun decode(buffer: RegistryFriendlyByteBuf): ArachnodeMount {
					return when (ByteBufCodecs.VAR_INT.decode(buffer)) {
						LINE_TYPE_ID -> Line
						SURFACE_TYPE_ID -> Surface(
							BlockPos.STREAM_CODEC.decode(buffer),
							Direction.STREAM_CODEC.decode(buffer),
							AaronExtraStreamCodecs.VEC3.decode(buffer)
						)
						else -> error("Unknown arachnode mount type")
					}
				}

				override fun encode(buffer: RegistryFriendlyByteBuf, mount: ArachnodeMount) {
					when (mount) {
						Line -> ByteBufCodecs.VAR_INT.encode(buffer, LINE_TYPE_ID)
						is Surface -> {
							ByteBufCodecs.VAR_INT.encode(buffer, SURFACE_TYPE_ID)
							BlockPos.STREAM_CODEC.encode(buffer, mount.blockPosition)
							Direction.STREAM_CODEC.encode(buffer, mount.face)
							AaronExtraStreamCodecs.VEC3.encode(buffer, mount.contactPosition)
						}
					}
				}
			}
	}
}