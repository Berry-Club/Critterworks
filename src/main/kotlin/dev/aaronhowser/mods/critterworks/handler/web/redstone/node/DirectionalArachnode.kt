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

class DirectionalArachnode(
	override val uuid: UUID,
	override val position: Vec3,
	override val itemStack: ItemStack,
	override val inputPorts: List<ArachnodePort>,
	override val outputPorts: List<ArachnodePort>
) : Arachnode() {

	override fun calculateOutputPowers(
		inputPowers: Map<ArachnodePort, Int>
	): Map<ArachnodePort, Int> {
		var strongestPower = 0
		for (inputPort in inputPorts) {
			strongestPower = maxOf(strongestPower, inputPowers.getOrDefault(inputPort, 0))
		}

		val outputPowers: MutableMap<ArachnodePort, Int> = mutableMapOf()
		for (outputPort in outputPorts) outputPowers[outputPort] = strongestPower
		return outputPowers
	}

	companion object {
		const val TYPE = "directional_arachnode"
		const val TYPE_ID = 0

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
					ArachnodePort.CODEC.codec()
						.listOf()
						.fieldOf("input_ports")
						.forGetter(DirectionalArachnode::inputPorts),
					ArachnodePort.CODEC.codec()
						.listOf()
						.fieldOf("output_ports")
						.forGetter(DirectionalArachnode::outputPorts)
				).apply(instance, ::DirectionalArachnode)
			}

		val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, DirectionalArachnode> =
			StreamCodec.composite(
				UUIDUtil.STREAM_CODEC, DirectionalArachnode::uuid,
				AaronExtraStreamCodecs.VEC3, DirectionalArachnode::position,
				ItemStack.OPTIONAL_STREAM_CODEC, DirectionalArachnode::itemStack,
				ArachnodePort.STREAM_CODEC.apply(ByteBufCodecs.list()), DirectionalArachnode::inputPorts,
				ArachnodePort.STREAM_CODEC.apply(ByteBufCodecs.list()), DirectionalArachnode::outputPorts,
				::DirectionalArachnode
			)
	}
}