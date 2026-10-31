package dev.aaronhowser.mods.critterworks.handler.web.redstone.node

import com.mojang.serialization.Codec
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3
import java.util.*

abstract class Arachnode {

	abstract val uuid: UUID
	abstract val position: Vec3
	abstract val itemStack: ItemStack
	abstract val mount: ArachnodeMount
	abstract val inputPorts: List<ArachnodePort>
	abstract val outputPorts: List<ArachnodePort>

	fun getPorts(): List<ArachnodePort> {
		return buildList {
			addAll(inputPorts)
			addAll(outputPorts)
		}
	}

	fun isLoaded(level: ServerLevel): Boolean {
		return getPorts().any { it.isLoaded(level) }
	}

	fun getDisplayDirection(): Vec3? {
		val inputCenter = getPortCenter(inputPorts) ?: return null
		val outputCenter = getPortCenter(outputPorts) ?: return null

		val direction = outputCenter.subtract(inputCenter)
		if (direction.lengthSqr() == 0.0) return null

		return direction.normalize()
	}

	abstract fun calculateOutputPowers(
		inputPowers: Map<ArachnodePort, Int>
	): Map<ArachnodePort, Int>

	private fun getPortCenter(ports: List<ArachnodePort>): Vec3? {
		if (ports.isEmpty()) return null

		val total = ports.fold(Vec3.ZERO) { currentTotal, port ->
			currentTotal.add(port.position)
		}

		return total.scale(1.0 / ports.size)
	}

	companion object {
		val CODEC: Codec<Arachnode> = Codec.STRING.dispatch(
			"type",
			{ arachnode ->
				when (arachnode) {
					is DirectionalArachnode -> DirectionalArachnode.TYPE
					is InvertingArachnode -> InvertingArachnode.TYPE
					else -> error("Unknown arachnode: ${arachnode::class.simpleName}")
				}
			},
			{ type ->
				when (type) {
					DirectionalArachnode.TYPE -> DirectionalArachnode.CODEC
					InvertingArachnode.TYPE -> InvertingArachnode.CODEC
					else -> error("Unknown arachnode type: $type")
				}
			}
		)

		val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, Arachnode> =
			object : StreamCodec<RegistryFriendlyByteBuf, Arachnode> {
				override fun decode(buffer: RegistryFriendlyByteBuf): Arachnode {
					return when (ByteBufCodecs.VAR_INT.decode(buffer)) {
						DirectionalArachnode.TYPE_ID -> DirectionalArachnode.STREAM_CODEC.decode(buffer)
						InvertingArachnode.TYPE_ID -> InvertingArachnode.STREAM_CODEC.decode(buffer)
						else -> error("Unknown arachnode type")
					}
				}

				override fun encode(buffer: RegistryFriendlyByteBuf, arachnode: Arachnode) {
					when (arachnode) {
						is DirectionalArachnode -> {
							ByteBufCodecs.VAR_INT.encode(buffer, DirectionalArachnode.TYPE_ID)
							DirectionalArachnode.STREAM_CODEC.encode(buffer, arachnode)
						}

						is InvertingArachnode -> {
							ByteBufCodecs.VAR_INT.encode(buffer, InvertingArachnode.TYPE_ID)
							InvertingArachnode.STREAM_CODEC.encode(buffer, arachnode)
						}

						else -> error("Unknown arachnode: ${arachnode::class.simpleName}")
					}
				}
			}
	}
}