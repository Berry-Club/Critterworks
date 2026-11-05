package dev.aaronhowser.mods.critterworks.item.component

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isFluid
import dev.aaronhowser.mods.aaron.serialization.AaronExtraStreamCodecs
import io.netty.buffer.ByteBuf
import net.minecraft.core.HolderLookup
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.Registries
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.tags.TagKey
import net.minecraft.util.StringRepresentable
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.material.Fluid
import net.neoforged.neoforge.fluids.FluidStack
import net.neoforged.neoforge.fluids.FluidUtil

sealed interface FluidFilterEntry {

	fun getDisplayStack(registries: HolderLookup.Provider): ItemStack
	fun test(stack: FluidStack): Boolean
	fun getType(): Type

	companion object {
		val CODEC: Codec<FluidFilterEntry> =
			Type.CODEC.dispatch(FluidFilterEntry::getType, Type::codec)
		val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, FluidFilterEntry> =
			Type.STREAM_CODEC
				.cast<RegistryFriendlyByteBuf>()
				.dispatch(FluidFilterEntry::getType) { type -> type.streamCodec }
	}

	enum class Type(
		val id: String,
		val codec: MapCodec<out FluidFilterEntry>,
		val streamCodec: StreamCodec<in RegistryFriendlyByteBuf, out FluidFilterEntry>
	) : StringRepresentable {
		EMPTY("empty", Empty.CODEC, Empty.STREAM_CODEC),
		TAG("tag", Tag.CODEC, Tag.STREAM_CODEC),
		FLUID("fluid", SpecificFluid.CODEC, SpecificFluid.STREAM_CODEC);

		override fun getSerializedName(): String = id

		companion object {
			val CODEC: Codec<Type> =
				StringRepresentable.fromEnum(Type::values)
			val STREAM_CODEC: StreamCodec<ByteBuf, Type> =
				AaronExtraStreamCodecs.enumStreamCodec(Type::class.java)
		}
	}

	data object Empty : FluidFilterEntry {
		override fun getDisplayStack(registries: HolderLookup.Provider): ItemStack = ItemStack.EMPTY
		override fun test(stack: FluidStack): Boolean = false
		override fun getType(): Type = Type.EMPTY

		val CODEC: MapCodec<Empty> = MapCodec.unit(Empty)
		val STREAM_CODEC: StreamCodec<ByteBuf, Empty> = StreamCodec.unit(Empty)
	}

	data class Tag(
		val tagKey: TagKey<Fluid>,
		val backupStack: ItemStack
	) : FluidFilterEntry {

		override fun getDisplayStack(registries: HolderLookup.Provider): ItemStack {
			val displayStack = backupStack.copy()
			displayStack.set(DataComponents.CUSTOM_NAME, Component.literal("#${tagKey.location()}"))

			return displayStack
		}

		override fun test(stack: FluidStack): Boolean = stack.isFluid(tagKey)
		override fun getType(): Type = Type.TAG

		fun getNextTag(): TagKey<Fluid> {
			val fluidStack = FluidUtil.getFluidContained(backupStack).orElse(FluidStack.EMPTY)
			val tags = fluidStack.tags.toList()
			if (tags.isEmpty()) return tagKey

			val currentIndex = tags.indexOf(tagKey)

			return tags[(currentIndex + 1) % tags.size]
		}

		fun asSpecificFluid(): SpecificFluid {
			return SpecificFluid(backupStack, false)
		}

		companion object {
			val CODEC: MapCodec<Tag> =
				RecordCodecBuilder.mapCodec { instance ->
					instance.group(
						TagKey.codec(Registries.FLUID)
							.fieldOf("tag")
							.forGetter(Tag::tagKey),
						ItemStack.CODEC
							.fieldOf("backup_stack")
							.forGetter(Tag::backupStack)
					).apply(instance, ::Tag)
				}

			val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, Tag> =
				StreamCodec.composite(
					AaronExtraStreamCodecs.tagKeyStreamCodec(Registries.FLUID),
					Tag::tagKey,
					ItemStack.STREAM_CODEC,
					Tag::backupStack,
					::Tag
				)
		}
	}

	data class SpecificFluid(
		val containerStack: ItemStack,
		val requireSameComponents: Boolean
	) : FluidFilterEntry {

		override fun getDisplayStack(registries: HolderLookup.Provider): ItemStack = containerStack

		override fun test(stack: FluidStack): Boolean {
			val filterFluid = FluidUtil.getFluidContained(containerStack).orElse(FluidStack.EMPTY)
			if (filterFluid.isEmpty) return false

			return if (requireSameComponents) {
				FluidStack.isSameFluidSameComponents(filterFluid, stack)
			} else {
				FluidStack.isSameFluid(filterFluid, stack)
			}
		}

		override fun getType(): Type = Type.FLUID

		companion object {
			val CODEC: MapCodec<SpecificFluid> =
				RecordCodecBuilder.mapCodec { instance ->
					instance.group(
						ItemStack.CODEC
							.fieldOf("container")
							.forGetter(SpecificFluid::containerStack),
						Codec.BOOL
							.optionalFieldOf("require_same_components", false)
							.forGetter(SpecificFluid::requireSameComponents)
					).apply(instance, ::SpecificFluid)
				}

			val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, SpecificFluid> =
				StreamCodec.composite(
					ItemStack.STREAM_CODEC, SpecificFluid::containerStack,
					ByteBufCodecs.BOOL, SpecificFluid::requireSameComponents,
					::SpecificFluid
				)
		}
	}

}