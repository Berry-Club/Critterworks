package dev.aaronhowser.mods.critterworks.handler.web.redstone.node

import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import dev.aaronhowser.mods.aaron.serialization.AaronExtraStreamCodecs
import dev.aaronhowser.mods.critterworks.handler.web.redstone.RedstoneWebLine
import net.minecraft.core.UUIDUtil
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3
import java.util.*

data class InvertingArachnode(
	override val uuid: UUID,
	override val position: Vec3,
	override val itemStack: ItemStack,
	override val inputLineUuid: UUID
) : Arachnode() {

	override fun canPower(sourceLine: RedstoneWebLine, destinationLine: RedstoneWebLine): Boolean {
		return false
	}

	override fun getGeneratedPower(
		destinationLine: RedstoneWebLine,
		linePowers: Map<UUID, Int>
	): Int {
		if (isInputLine(destinationLine)) return 0
		if (linePowers.getOrDefault(inputLineUuid, 0) > 0) return 0

		return MAX_POWER
	}

	companion object {
		const val TYPE = "inverting_arachnode"
		const val TYPE_ID = 3
		private const val MAX_POWER = 15

		val CODEC: MapCodec<InvertingArachnode> =
			RecordCodecBuilder.mapCodec { instance ->
				instance.group(
					UUIDUtil.CODEC
						.fieldOf("uuid")
						.forGetter(InvertingArachnode::uuid),
					Vec3.CODEC
						.fieldOf("position")
						.forGetter(InvertingArachnode::position),
					ItemStack.OPTIONAL_CODEC
						.fieldOf("item_stack")
						.forGetter(InvertingArachnode::itemStack),
					UUIDUtil.CODEC
						.fieldOf("input_line_uuid")
						.forGetter(InvertingArachnode::inputLineUuid)
				).apply(instance, ::InvertingArachnode)
			}

		val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, InvertingArachnode> =
			StreamCodec.composite(
				UUIDUtil.STREAM_CODEC, InvertingArachnode::uuid,
				AaronExtraStreamCodecs.VEC3, InvertingArachnode::position,
				ItemStack.OPTIONAL_STREAM_CODEC, InvertingArachnode::itemStack,
				UUIDUtil.STREAM_CODEC, InvertingArachnode::inputLineUuid,
				::InvertingArachnode
			)
	}
}