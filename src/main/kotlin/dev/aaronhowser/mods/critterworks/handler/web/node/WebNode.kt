package dev.aaronhowser.mods.critterworks.handler.web.node

import com.mojang.serialization.Codec
import dev.aaronhowser.mods.critterworks.handler.web.line.WebLine
import dev.aaronhowser.mods.critterworks.handler.web.line.WebLineInvalidation
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.phys.Vec3
import java.util.*

sealed class WebNode {

	abstract val uuid: UUID
	abstract val position: Vec3

	val lines: Set<WebLine>
		field = mutableSetOf()

	abstract fun isLoaded(level: ServerLevel): Boolean
	abstract fun getInvalidation(
		level: ServerLevel,
		lines: Map<UUID, WebLine>,
		checkedLines: MutableSet<UUID>
	): WebLineInvalidation?

	fun addLine(line: WebLine) {
		lines.add(line)
	}

	fun removeLine(line: WebLine) {
		lines.remove(line)
	}

	companion object {
		fun isChunkLoaded(level: ServerLevel, chunkPos: ChunkPos): Boolean {
			return level.hasChunk(chunkPos.x, chunkPos.z)
		}

		val CODEC: Codec<WebNode> =
			Codec.STRING.dispatch(
				"type",
				{ node ->
					when (node) {
						is WebBlockAnchor -> WebBlockAnchor.TYPE
						is WebLineAnchor -> WebLineAnchor.TYPE
					}
				},
				{ type ->
					when (type) {
						WebBlockAnchor.TYPE -> WebBlockAnchor.CODEC
						WebLineAnchor.TYPE -> WebLineAnchor.CODEC
						else -> error("Unknown web node type: $type")
					}
				}
			)

		val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, WebNode> =
			object : StreamCodec<RegistryFriendlyByteBuf, WebNode> {
				override fun decode(buffer: RegistryFriendlyByteBuf): WebNode {
					return when (ByteBufCodecs.VAR_INT.decode(buffer)) {
						WebBlockAnchor.TYPE_ID -> WebBlockAnchor.STREAM_CODEC.decode(buffer)
						WebLineAnchor.TYPE_ID -> WebLineAnchor.STREAM_CODEC.decode(buffer)
						else -> error("Unknown web node type")
					}
				}

				override fun encode(buffer: RegistryFriendlyByteBuf, node: WebNode) {
					when (node) {
						is WebBlockAnchor -> {
							ByteBufCodecs.VAR_INT.encode(buffer, WebBlockAnchor.TYPE_ID)
							WebBlockAnchor.STREAM_CODEC.encode(buffer, node)
						}

						is WebLineAnchor -> {
							ByteBufCodecs.VAR_INT.encode(buffer, WebLineAnchor.TYPE_ID)
							WebLineAnchor.STREAM_CODEC.encode(buffer, node)
						}
					}
				}
			}
	}

}