package dev.aaronhowser.mods.critterworks.handler.web.common.node

import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import dev.aaronhowser.mods.aaron.serialization.AaronExtraStreamCodecs
import dev.aaronhowser.mods.critterworks.handler.web.common.line.WebLine
import dev.aaronhowser.mods.critterworks.handler.web.common.line.WebLineInvalidation
import dev.aaronhowser.mods.critterworks.handler.web.common.line.WebLineInvalidationReason
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.UUIDUtil
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.phys.Vec3
import java.util.*

class WebBlockAnchor(
	override val uuid: UUID,
	val blockPos: BlockPos,
	val face: Direction,
	override val position: Vec3
) : WebNode() {

	override fun isLoaded(level: ServerLevel): Boolean {
		return isChunkLoaded(level, ChunkPos(blockPos))
	}

	override fun getInvalidation(
		level: ServerLevel,
		lines: Map<UUID, WebLine>,
		checkedLines: MutableSet<UUID>
	): WebLineInvalidation? {
		val collisionShape = level.getBlockState(blockPos)
			.getCollisionShape(level, blockPos)
		if (!collisionShape.isEmpty) return null

		return WebLineInvalidation(WebLineInvalidationReason.INVALID_ANCHOR)
	}

	companion object {
		const val TYPE = "block"
		const val TYPE_ID = 0

		fun create(blockPos: BlockPos, face: Direction, position: Vec3): WebBlockAnchor {
			val surfaceOffset = 0.001
			val faceNormal = Vec3.atLowerCornerOf(face.normal)

			return WebBlockAnchor(
				UUID.randomUUID(),
				blockPos,
				face,
				position.add(faceNormal.scale(surfaceOffset))
			)
		}

		val CODEC: MapCodec<WebBlockAnchor> =
			RecordCodecBuilder.mapCodec { instance ->
				instance.group(
					UUIDUtil.CODEC
						.fieldOf("uuid")
						.forGetter(WebBlockAnchor::uuid),
					BlockPos.CODEC
						.fieldOf("block_pos")
						.forGetter(WebBlockAnchor::blockPos),
					Direction.CODEC
						.fieldOf("face")
						.forGetter(WebBlockAnchor::face),
					Vec3.CODEC
						.fieldOf("position")
						.forGetter(WebBlockAnchor::position)
				).apply(instance, ::WebBlockAnchor)
			}

		val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, WebBlockAnchor> = StreamCodec.composite(
			UUIDUtil.STREAM_CODEC, WebBlockAnchor::uuid,
			BlockPos.STREAM_CODEC, WebBlockAnchor::blockPos,
			Direction.STREAM_CODEC, WebBlockAnchor::face,
			AaronExtraStreamCodecs.VEC3, WebBlockAnchor::position,
			::WebBlockAnchor
		)
	}
}