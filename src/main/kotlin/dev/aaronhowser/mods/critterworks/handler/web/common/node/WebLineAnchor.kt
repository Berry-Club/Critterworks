package dev.aaronhowser.mods.critterworks.handler.web.common.node

import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import dev.aaronhowser.mods.aaron.serialization.AaronExtraStreamCodecs
import dev.aaronhowser.mods.critterworks.handler.web.common.line.WebLine
import dev.aaronhowser.mods.critterworks.handler.web.common.line.WebLineInvalidation
import dev.aaronhowser.mods.critterworks.handler.web.common.line.WebLineInvalidationReason
import net.minecraft.core.BlockPos
import net.minecraft.core.UUIDUtil
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.phys.Vec3
import java.util.*

class WebLineAnchor(
	override val uuid: UUID,
	var parentLineUuid: UUID,
	override val position: Vec3
) : WebNode() {

	override fun isLoaded(level: ServerLevel): Boolean {
		val chunkPos = ChunkPos(BlockPos.containing(position))
		return isChunkLoaded(level, chunkPos)
	}

	override fun findInvalidation(
		level: ServerLevel,
		lines: Map<UUID, WebLine>,
		checkedLines: MutableSet<UUID>
	): WebLineInvalidation? {
		val line = lines[parentLineUuid]
			?: return WebLineInvalidation(
				WebLineInvalidationReason.MISSING_LINE,
				dependencyDepth = 1
			)
		if (!line.isLoaded(level)) return null

		val invalidation = line.getInvalidation(level, lines, checkedLines) ?: return null
		return invalidation.copy(dependencyDepth = invalidation.dependencyDepth + 1)
	}

	companion object {
		const val TYPE = "line"
		const val TYPE_ID = 1

		val CODEC: MapCodec<WebLineAnchor> =
			RecordCodecBuilder.mapCodec { instance ->
				instance.group(
					UUIDUtil.CODEC
						.fieldOf("uuid")
						.forGetter(WebLineAnchor::uuid),
					UUIDUtil.CODEC
						.fieldOf("line_uuid")
						.forGetter(WebLineAnchor::parentLineUuid),
					Vec3.CODEC
						.fieldOf("position")
						.forGetter(WebLineAnchor::position)
				).apply(instance, ::WebLineAnchor)
			}

		val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, WebLineAnchor> = StreamCodec.composite(
			UUIDUtil.STREAM_CODEC, WebLineAnchor::uuid,
			UUIDUtil.STREAM_CODEC, WebLineAnchor::parentLineUuid,
			AaronExtraStreamCodecs.VEC3, WebLineAnchor::position,
			::WebLineAnchor
		)
	}
}