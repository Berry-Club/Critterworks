package dev.aaronhowser.mods.critterworks.handler.web.redstone.node

import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import dev.aaronhowser.mods.aaron.serialization.AaronExtraStreamCodecs
import dev.aaronhowser.mods.critterworks.handler.web.common.line.WebLine
import dev.aaronhowser.mods.critterworks.handler.web.common.line.WebLineInvalidation
import dev.aaronhowser.mods.critterworks.handler.web.common.node.WebNode
import net.minecraft.core.BlockPos
import net.minecraft.core.UUIDUtil
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.phys.Vec3
import java.util.*

class ArachnodePort(
	override val uuid: UUID,
	val arachnodeUuid: UUID,
	override val position: Vec3
) : WebNode() {

	override fun isLoaded(level: ServerLevel): Boolean {
		return isChunkLoaded(level, ChunkPos(BlockPos.containing(position)))
	}

	override fun findInvalidation(
		level: ServerLevel,
		lines: Map<UUID, WebLine>,
		checkedLines: MutableSet<UUID>
	): WebLineInvalidation? {
		return null
	}

	companion object {
		const val TYPE = "arachnode_port"
		const val TYPE_ID = 2

		val CODEC: MapCodec<ArachnodePort> = RecordCodecBuilder.mapCodec { instance ->
			instance.group(
				UUIDUtil.CODEC
					.fieldOf("uuid")
					.forGetter(ArachnodePort::uuid),
				UUIDUtil.CODEC
					.fieldOf("arachnode_uuid")
					.forGetter(ArachnodePort::arachnodeUuid),
				Vec3.CODEC
					.fieldOf("position")
					.forGetter(ArachnodePort::position)
			).apply(instance, ::ArachnodePort)
		}

		val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, ArachnodePort> = StreamCodec.composite(
			UUIDUtil.STREAM_CODEC, ArachnodePort::uuid,
			UUIDUtil.STREAM_CODEC, ArachnodePort::arachnodeUuid,
			AaronExtraStreamCodecs.VEC3, ArachnodePort::position,
			::ArachnodePort
		)
	}
}