package dev.aaronhowser.mods.critterworks.handler.web.redstone.node

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.phys.Vec3

data class ArachnodeMount(
	val blockPosition: BlockPos,
	val face: Direction,
	val isMounted: Boolean
) {

	fun getSurfaceNormal(): Vec3 {
		return Vec3.atLowerCornerOf(face.normal)
	}

	fun isValid(level: ServerLevel): Boolean {
		if (!isMounted) return true
		val chunkPosition = ChunkPos(blockPosition)
		if (!level.hasChunk(chunkPosition.x, chunkPosition.z)) return true

		return !level.getBlockState(blockPosition)
			.getCollisionShape(level, blockPosition)
			.isEmpty
	}

	companion object {
		val NONE = ArachnodeMount(BlockPos.ZERO, Direction.UP, false)

		val CODEC: Codec<ArachnodeMount> = RecordCodecBuilder.create { instance ->
			instance.group(
				BlockPos.CODEC
					.fieldOf("block_position")
					.forGetter(ArachnodeMount::blockPosition),
				Direction.CODEC
					.fieldOf("face")
					.forGetter(ArachnodeMount::face),
				Codec.BOOL
					.fieldOf("is_mounted")
					.forGetter(ArachnodeMount::isMounted)
			).apply(instance, ::ArachnodeMount)
		}

		val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, ArachnodeMount> = StreamCodec.composite(
			BlockPos.STREAM_CODEC, ArachnodeMount::blockPosition,
			Direction.STREAM_CODEC, ArachnodeMount::face,
			ByteBufCodecs.BOOL, ArachnodeMount::isMounted,
			::ArachnodeMount
		)
	}
}