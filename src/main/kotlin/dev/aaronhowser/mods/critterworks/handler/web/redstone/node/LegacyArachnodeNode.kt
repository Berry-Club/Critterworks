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
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.phys.Vec3
import java.util.*

class LegacyArachnodeNode(
	override val uuid: UUID,
	override val position: Vec3,
	val itemStack: ItemStack,
	val inputLineUuid: UUID,
	val type: LegacyArachnodeType
) : WebNode() {

	override fun isLoaded(level: ServerLevel): Boolean {
		return isChunkLoaded(level, ChunkPos(BlockPos.containing(position)))
	}

	override fun getInvalidation(
		level: ServerLevel,
		lines: Map<UUID, WebLine>,
		checkedLines: MutableSet<UUID>
	): WebLineInvalidation? {
		return null
	}

	companion object {
		const val DIRECTIONAL_TYPE = "directional_arachnode"
		const val DIRECTIONAL_TYPE_ID = 2
		const val INVERTING_TYPE = "inverting_arachnode"
		const val INVERTING_TYPE_ID = 3

		val DIRECTIONAL_CODEC: MapCodec<LegacyArachnodeNode> = createCodec(
			LegacyArachnodeType.DIRECTIONAL
		)
		val INVERTING_CODEC: MapCodec<LegacyArachnodeNode> = createCodec(
			LegacyArachnodeType.INVERTING
		)
		val DIRECTIONAL_STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, LegacyArachnodeNode> =
			createStreamCodec(LegacyArachnodeType.DIRECTIONAL)
		val INVERTING_STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, LegacyArachnodeNode> =
			createStreamCodec(LegacyArachnodeType.INVERTING)

		private fun createCodec(type: LegacyArachnodeType): MapCodec<LegacyArachnodeNode> {
			return RecordCodecBuilder.mapCodec { instance ->
				instance.group(
					UUIDUtil.CODEC
						.fieldOf("uuid")
						.forGetter(LegacyArachnodeNode::uuid),
					Vec3.CODEC
						.fieldOf("position")
						.forGetter(LegacyArachnodeNode::position),
					ItemStack.OPTIONAL_CODEC
						.fieldOf("item_stack")
						.forGetter(LegacyArachnodeNode::itemStack),
					UUIDUtil.CODEC
						.fieldOf("input_line_uuid")
						.forGetter(LegacyArachnodeNode::inputLineUuid)
				).apply(instance) { uuid, position, itemStack, inputLineUuid ->
					LegacyArachnodeNode(uuid, position, itemStack, inputLineUuid, type)
				}
			}
		}

		private fun createStreamCodec(
			type: LegacyArachnodeType
		): StreamCodec<RegistryFriendlyByteBuf, LegacyArachnodeNode> {
			return StreamCodec.composite(
				UUIDUtil.STREAM_CODEC, LegacyArachnodeNode::uuid,
				AaronExtraStreamCodecs.VEC3, LegacyArachnodeNode::position,
				ItemStack.OPTIONAL_STREAM_CODEC, LegacyArachnodeNode::itemStack,
				UUIDUtil.STREAM_CODEC, LegacyArachnodeNode::inputLineUuid,
				{ uuid, position, itemStack, inputLineUuid ->
					LegacyArachnodeNode(uuid, position, itemStack, inputLineUuid, type)
				}
			)
		}
	}
}