package dev.aaronhowser.mods.critterworks.item.component

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isItem
import dev.aaronhowser.mods.critterworks.item.FluidFilterItem
import dev.aaronhowser.mods.critterworks.registry.ModItems
import net.minecraft.core.NonNullList
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.world.level.material.Fluid
import net.neoforged.neoforge.fluids.FluidStack

data class FluidFilterComponent(
	val entries: NonNullList<FluidFilterEntry>,
	val isBlacklist: Boolean
) {

	constructor() : this(emptyList(), false)
	constructor(entries: List<FluidFilterEntry>, isBlacklist: Boolean) : this(sanitizeEntries(entries), isBlacklist)

	private val cache = mutableMapOf<CacheKey, Boolean>()

	fun test(testedStack: FluidStack): Boolean {
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

	private fun testEntry(entry: FluidFilterEntry, testedStack: FluidStack): Boolean {
		if (
			entry is FluidFilterEntry.SpecificFluid
			&& entry.containerStack.isItem(ModItems.FLUID_FILTERFEEDER)
		) {
			return FluidFilterItem.passesFilter(entry.containerStack, testedStack)
		}

		return entry.test(testedStack)
	}

	companion object {
		const val CONTAINER_SIZE = 9

		fun sanitizeEntries(entries: List<FluidFilterEntry>): NonNullList<FluidFilterEntry> {
			val sanitizedEntries = NonNullList.withSize<FluidFilterEntry>(CONTAINER_SIZE, FluidFilterEntry.Empty)

			for (index in 0 until CONTAINER_SIZE) {
				val entry = entries.getOrNull(index) ?: continue
				sanitizedEntries[index] = entry
			}

			return sanitizedEntries
		}

		val CODEC: Codec<FluidFilterComponent> = RecordCodecBuilder.create { instance ->
			instance.group(
				FluidFilterEntry.CODEC.listOf()
					.fieldOf("entries")
					.forGetter(FluidFilterComponent::trimmedEntries),
				Codec.BOOL
					.optionalFieldOf("is_blacklist", false)
					.forGetter(FluidFilterComponent::isBlacklist)
			).apply(instance, ::FluidFilterComponent)
		}

		val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, FluidFilterComponent> =
			StreamCodec.composite(
				FluidFilterEntry.STREAM_CODEC.apply(ByteBufCodecs.list()), FluidFilterComponent::entries,
				ByteBufCodecs.BOOL, FluidFilterComponent::isBlacklist,
				::FluidFilterComponent
			)

		private fun trimmedEntries(component: FluidFilterComponent): List<FluidFilterEntry> {
			var lastEntryIndex = -1
			for (index in component.entries.indices.reversed()) {
				if (component.entries[index] is FluidFilterEntry.Empty) continue

				lastEntryIndex = index
				break
			}

			return component.entries.take(lastEntryIndex + 1)
		}
	}

	data class CacheKey(val fluid: Fluid, val componentHash: Int) {
		constructor(stack: FluidStack) : this(stack.fluid, stack.componentsPatch.hashCode())
	}
}