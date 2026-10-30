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

data class DirectionalArachnode(
	override val uuid: UUID,
	override val position: Vec3,
	override val itemStack: ItemStack,
	val inputLineUuid: UUID
) : Arachnode() {

	override fun canPower(sourceLine: RedstoneWebLine, destinationLine: RedstoneWebLine): Boolean {
		if (sourceLine == destinationLine) return false

		val sourceIsInput = isInputLine(sourceLine)
		val destinationIsInput = isInputLine(destinationLine)
		return sourceIsInput || !destinationIsInput
	}

	fun isInputLine(line: RedstoneWebLine): Boolean {
		return line.uuid == inputLineUuid
	}

	fun getOutputDirection(): Vec3? {
		for (line in lines) {
			if (line !is RedstoneWebLine || isInputLine(line)) continue

			val outputNode = when (this) {
				line.firstNode -> line.secondNode
				line.secondNode -> line.firstNode
				else -> continue
			}
			val outputDirection = outputNode.position.subtract(position)
			if (outputDirection.lengthSqr() == 0.0) return null

			return outputDirection.normalize()
		}

		return null
	}

	companion object {
		const val TYPE = "directional_arachnode"
		const val TYPE_ID = 2

		val CODEC: MapCodec<DirectionalArachnode> =
			RecordCodecBuilder.mapCodec { instance ->
				instance.group(
					UUIDUtil.CODEC
						.fieldOf("uuid")
						.forGetter(DirectionalArachnode::uuid),
					Vec3.CODEC
						.fieldOf("position")
						.forGetter(DirectionalArachnode::position),
					ItemStack.OPTIONAL_CODEC
						.fieldOf("item_stack")
						.forGetter(DirectionalArachnode::itemStack),
					UUIDUtil.CODEC
						.fieldOf("input_line_uuid")
						.forGetter(DirectionalArachnode::inputLineUuid)
				).apply(instance, ::DirectionalArachnode)
			}

		val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, DirectionalArachnode> =
			StreamCodec.composite(
				UUIDUtil.STREAM_CODEC, DirectionalArachnode::uuid,
				AaronExtraStreamCodecs.VEC3, DirectionalArachnode::position,
				ItemStack.OPTIONAL_STREAM_CODEC, DirectionalArachnode::itemStack,
				UUIDUtil.STREAM_CODEC, DirectionalArachnode::inputLineUuid,
				::DirectionalArachnode
			)
	}
}