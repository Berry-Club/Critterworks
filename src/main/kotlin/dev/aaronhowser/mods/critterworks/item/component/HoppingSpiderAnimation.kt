package dev.aaronhowser.mods.critterworks.item.component

import com.mojang.serialization.Codec
import io.netty.buffer.ByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.util.StringRepresentable

enum class HoppingSpiderAnimation(
	val animationName: String
) : StringRepresentable {
	IDLE("animation.hopping_spider.idle"),
	IDLE_HOLDING("animation.hopping_spider.idle_holding"),
	WALK("animation.hopping_spider.walk"),
	WALK_HOLDING("animation.hopping_spider.walk_holding");

	override fun getSerializedName(): String = animationName

	companion object {
		val CODEC: Codec<HoppingSpiderAnimation> = StringRepresentable.fromEnum(::values)

		val STREAM_CODEC: StreamCodec<ByteBuf, HoppingSpiderAnimation> =
			ByteBufCodecs.idMapper(
				HoppingSpiderAnimation::fromOrdinal,
				HoppingSpiderAnimation::ordinal
			)

		fun fromState(isWalking: Boolean, isHoldingItem: Boolean): HoppingSpiderAnimation {
			return when {
				isWalking && isHoldingItem -> WALK_HOLDING
				isWalking -> WALK
				isHoldingItem -> IDLE_HOLDING
				else -> IDLE
			}
		}

		private fun fromOrdinal(ordinal: Int): HoppingSpiderAnimation {
			return entries.getOrElse(ordinal) { IDLE }
		}
	}
}