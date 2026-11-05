package dev.aaronhowser.mods.critterworks.item.component

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isItem
import dev.aaronhowser.mods.critterworks.item.ItemFilterItem
import dev.aaronhowser.mods.critterworks.registry.ModItems
import net.minecraft.core.NonNullList
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack

data class ItemFilterComponent(
	val entries: NonNullList<ItemFilterEntry>,
	val isBlacklist: Boolean
) {

	constructor() : this(emptyList(), false)
	constructor(entries: List<ItemFilterEntry>, isBlacklist: Boolean) : this(sanitizeEntries(entries), isBlacklist)

	private val cache = mutableMapOf<CacheKey, Boolean>()

	fun test(testedStack: ItemStack): Boolean {
		if (testedStack.isEmpty) return isBlacklist

		val cacheKey = CacheKey(testedStack)
		val cachedResult = cache[cacheKey]
		if (cachedResult != null) return cachedResult

		var passes = false
		for (entry in entries) {
			if (!testEntry(entry, testedStack)) continue

			passes = true
			break
		}
		val result = passes != isBlacklist
		cache[cacheKey] = result

		return result
	}

	private fun testEntry(entry: ItemFilterEntry, testedStack: ItemStack): Boolean {
		if (entry is ItemFilterEntry.SpecificItem && entry.stack.isItem(ModItems.ITEM_FILTERFEEDER)) {
			return ItemFilterItem.passesFilter(entry.stack, testedStack)
		}

		return entry.test(testedStack)
	}

	companion object {
		const val CONTAINER_SIZE = 9

		fun sanitizeEntries(entries: List<ItemFilterEntry>): NonNullList<ItemFilterEntry> {
			val sanitizedEntries = NonNullList.withSize<ItemFilterEntry>(CONTAINER_SIZE, ItemFilterEntry.Empty)

			for (index in 0 until CONTAINER_SIZE) {
				val entry = entries.getOrNull(index) ?: continue
				sanitizedEntries[index] = entry
			}

			return sanitizedEntries
		}

		val CODEC: Codec<ItemFilterComponent> = RecordCodecBuilder.create { instance ->
			instance.group(
				ItemFilterEntry.CODEC.listOf()
					.fieldOf("entries")
					.forGetter(ItemFilterComponent::trimmedEntries),
				Codec.BOOL
					.optionalFieldOf("is_blacklist", false)
					.forGetter(ItemFilterComponent::isBlacklist)
			).apply(instance, ::ItemFilterComponent)
		}

		val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, ItemFilterComponent> =
			StreamCodec.composite(
				ItemFilterEntry.STREAM_CODEC.apply(ByteBufCodecs.list()), ItemFilterComponent::entries,
				ByteBufCodecs.BOOL, ItemFilterComponent::isBlacklist,
				::ItemFilterComponent
			)

		private fun trimmedEntries(component: ItemFilterComponent): List<ItemFilterEntry> {
			var lastEntryIndex = -1
			for (index in component.entries.indices.reversed()) {
				if (component.entries[index] is ItemFilterEntry.Empty) continue

				lastEntryIndex = index
				break
			}

			return component.entries.take(lastEntryIndex + 1)
		}
	}

	data class CacheKey(val item: Item, val componentHash: Int) {
		constructor(stack: ItemStack) : this(stack.item, stack.componentsPatch.hashCode())
	}
}