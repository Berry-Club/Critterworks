package dev.aaronhowser.mods.critterworks.entity.scoochworm.data

import com.mojang.serialization.Codec
import dev.aaronhowser.mods.aaron.serialization.AaronExtraStreamCodecs
import dev.aaronhowser.mods.critterworks.Critterworks
import dev.aaronhowser.mods.critterworks.registry.ModItems
import io.netty.buffer.ByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.util.StringRepresentable
import net.minecraft.world.item.ItemStack

enum class WormColor(
	val colorName: String,
	val tintColor: Int
) : StringRepresentable {
	GREEN("green", 0xFF55FF55.toInt()),
	BLUE("blue", 0xFF5555FF.toInt()),
	RED("red", 0xFFFF5555.toInt()),
	YELLOW("yellow", 0xFFFFFF55.toInt()),
	MAGENTA("magenta", 0xFFFF55FF.toInt()),
	CYAN("cyan", 0xFF55FFFF.toInt()),
	AARON("aaron", 0xFF5555FF.toInt());

	val headTexture = Critterworks.modResource("textures/entity/scoochworm/$colorName/head.png")
	val bodyTexture = Critterworks.modResource("textures/entity/scoochworm/$colorName/body.png")

	override fun getSerializedName(): String = colorName

	fun next(): WormColor {
		return entries[(ordinal + 1) % entries.size]
	}

	fun getDyeberryStack(): ItemStack {
		return when (this) {
			GREEN -> ModItems.GREEN_DYEBERRY
			BLUE -> ModItems.BLUE_DYEBERRY
			RED -> ModItems.RED_DYEBERRY
			YELLOW -> ModItems.YELLOW_DYEBERRY
			MAGENTA -> ModItems.MAGENTA_DYEBERRY
			CYAN -> ModItems.CYAN_DYEBERRY
			AARON -> ModItems.AARONBERRY
		}.toStack()
	}

	companion object {
		val CODEC: Codec<WormColor> =
			StringRepresentable.fromEnum { entries.toTypedArray() }

		val STREAM_CODEC: StreamCodec<ByteBuf, WormColor> =
			AaronExtraStreamCodecs.enumStreamCodec(WormColor::class.java)

		fun fromOrdinal(ordinal: Int): WormColor {
			return entries.getOrElse(ordinal) { GREEN }
		}
	}
}