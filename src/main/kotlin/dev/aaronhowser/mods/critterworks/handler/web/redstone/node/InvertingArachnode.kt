package dev.aaronhowser.mods.critterworks.handler.web.redstone.node

import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import dev.aaronhowser.mods.aaron.serialization.AaronExtraStreamCodecs
import net.minecraft.core.UUIDUtil
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3
import java.util.*

class InvertingArachnode(
	override val uuid: UUID,
	override val position: Vec3,
	override val itemStack: ItemStack,
	override val mount: ArachnodeMount,
	override val inputPorts: List<ArachnodePort>,
	override val outputPorts: List<ArachnodePort>
) : Arachnode() {

	override fun calculateOutputPowers(
		inputPowers: Map<ArachnodePort, Int>
	): Map<ArachnodePort, Int> {
		var hasPoweredInput = false
		for (inputPort in inputPorts) {
			if (inputPowers.getOrDefault(inputPort, 0) <= 0) continue

			hasPoweredInput = true
			break
		}

		val outputPower = if (hasPoweredInput) 0 else MAX_POWER
		val outputPowers: MutableMap<ArachnodePort, Int> = mutableMapOf()
		for (outputPort in outputPorts) outputPowers[outputPort] = outputPower
		return outputPowers
	}

	companion object {
		const val TYPE = "inverting_arachnode"
		const val TYPE_ID = 1
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
					ArachnodeMount.CODEC
						.optionalFieldOf("mount", ArachnodeMount.NONE)
						.forGetter(InvertingArachnode::mount),
					ArachnodePort.CODEC.codec()
						.listOf()
						.fieldOf("input_ports")
						.forGetter(InvertingArachnode::inputPorts),
					ArachnodePort.CODEC.codec()
						.listOf()
						.fieldOf("output_ports")
						.forGetter(InvertingArachnode::outputPorts)
				).apply(instance, ::InvertingArachnode)
			}

		val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, InvertingArachnode> =
			StreamCodec.composite(
				UUIDUtil.STREAM_CODEC, InvertingArachnode::uuid,
				AaronExtraStreamCodecs.VEC3, InvertingArachnode::position,
				ItemStack.OPTIONAL_STREAM_CODEC, InvertingArachnode::itemStack,
				ArachnodeMount.STREAM_CODEC, InvertingArachnode::mount,
				ArachnodePort.STREAM_CODEC.apply(ByteBufCodecs.list()), InvertingArachnode::inputPorts,
				ArachnodePort.STREAM_CODEC.apply(ByteBufCodecs.list()), InvertingArachnode::outputPorts,
				::InvertingArachnode
			)
	}
}