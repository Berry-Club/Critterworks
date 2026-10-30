package dev.aaronhowser.mods.critterworks.handler.web.common.node

import com.mojang.serialization.Codec
import dev.aaronhowser.mods.critterworks.handler.web.common.line.WebLine
import dev.aaronhowser.mods.critterworks.handler.web.common.line.WebLineInvalidation
import dev.aaronhowser.mods.critterworks.handler.web.redstone.node.ArachnodePort
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.phys.Vec3
import java.util.*

abstract class WebNode {

	abstract val uuid: UUID
	abstract val position: Vec3

	val lines: Set<WebLine>
		field = mutableSetOf()

	abstract fun isLoaded(level: ServerLevel): Boolean
	abstract fun findInvalidation(
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
						is ArachnodePort -> ArachnodePort.TYPE
						else -> error("Unknown web node: ${node::class.simpleName}")
					}
				},
				{ type ->
					when (type) {
						WebBlockAnchor.TYPE -> WebBlockAnchor.CODEC
						WebLineAnchor.TYPE -> WebLineAnchor.CODEC
						ArachnodePort.TYPE -> ArachnodePort.CODEC
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
						ArachnodePort.TYPE_ID -> ArachnodePort.STREAM_CODEC.decode(buffer)
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

						is ArachnodePort -> {
							ByteBufCodecs.VAR_INT.encode(buffer, ArachnodePort.TYPE_ID)
							ArachnodePort.STREAM_CODEC.encode(buffer, node)
						}

						else -> error("Unknown web node: ${node::class.simpleName}")
					}
				}
			}
	}

}