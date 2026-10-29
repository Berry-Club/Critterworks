package dev.aaronhowser.mods.critterworks.handler.web.item

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.core.UUIDUtil
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.world.item.ItemStack
import java.util.*

data class ItemWebPortData(
	val nodeUuid: UUID,
	val stack: ItemStack
) {
	companion object {
		val CODEC: Codec<ItemWebPortData> = RecordCodecBuilder.create { instance ->
			instance.group(
				UUIDUtil.CODEC
					.fieldOf("uuid")
					.forGetter(ItemWebPortData::nodeUuid),
				ItemStack.OPTIONAL_CODEC
					.optionalFieldOf("web_port", ItemStack.EMPTY)
					.forGetter(ItemWebPortData::stack)
			).apply(instance, ::ItemWebPortData)
		}

		val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, ItemWebPortData> = StreamCodec.composite(
			UUIDUtil.STREAM_CODEC, ItemWebPortData::nodeUuid,
			ItemStack.OPTIONAL_STREAM_CODEC, ItemWebPortData::stack,
			::ItemWebPortData
		)
	}
}