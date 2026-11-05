package dev.aaronhowser.mods.critterworks.item.component

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isItem
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isSameItemSameComponents
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.toComponent
import dev.aaronhowser.mods.aaron.serialization.AaronExtraStreamCodecs
import dev.aaronhowser.mods.critterworks.datagen.language.ModMenuLang
import io.netty.buffer.ByteBuf
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap
import net.minecraft.core.HolderLookup
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.Registries
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.network.codec.StreamCodec
import net.minecraft.tags.TagKey
import net.minecraft.util.StringRepresentable
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import java.util.*
import kotlin.random.Random

sealed interface ItemFilterEntry {

	fun getDisplayStack(registries: HolderLookup.Provider): ItemStack
	fun test(stack: ItemStack): Boolean
	fun getType(): Type

	companion object {
		val CODEC: Codec<ItemFilterEntry> = Type.CODEC.dispatch(ItemFilterEntry::getType, Type::codec)
		val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, ItemFilterEntry> =
			Type.STREAM_CODEC
				.cast<RegistryFriendlyByteBuf>()
				.dispatch(ItemFilterEntry::getType) { type -> type.streamCodec }
	}

	enum class Type(
		val id: String,
		val codec: MapCodec<out ItemFilterEntry>,
		val streamCodec: StreamCodec<in RegistryFriendlyByteBuf, out ItemFilterEntry>
	) : StringRepresentable {
		EMPTY("empty", Empty.CODEC, Empty.STREAM_CODEC),
		TAG("tag", Tag.CODEC, Tag.STREAM_CODEC),
		ITEM("item", SpecificItem.CODEC, SpecificItem.STREAM_CODEC);

		override fun getSerializedName(): String = id

		companion object {
			val CODEC: Codec<Type> =
				StringRepresentable.fromEnum(Type::values)
			val STREAM_CODEC: StreamCodec<ByteBuf, Type> =
				AaronExtraStreamCodecs.enumStreamCodec(Type::class.java)
		}
	}

	data object Empty : ItemFilterEntry {
		override fun getDisplayStack(registries: HolderLookup.Provider): ItemStack = ItemStack.EMPTY
		override fun test(stack: ItemStack): Boolean = false
		override fun getType(): Type = Type.EMPTY

		val CODEC: MapCodec<Empty> = MapCodec.unit(Empty)
		val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, Empty> = StreamCodec.unit(Empty)
	}

	data class Tag(
		val tagKey: TagKey<Item>,
		val backupStack: ItemStack
	) : ItemFilterEntry {
		private val displayStacks: MutableMap<Item, ItemStack> = mutableMapOf()
		private var displayStack: ItemStack? = null
		private var timeLastUpdated = 0L

		override fun getDisplayStack(registries: HolderLookup.Provider): ItemStack {
			val currentTime = System.currentTimeMillis() / 1000
			if (displayStack == null || currentTime > timeLastUpdated) {
				timeLastUpdated = currentTime

				val matchingTag = registries.lookupOrThrow(Registries.ITEM)
					.get(tagKey)
					.orElse(null)
					?: return ItemStack.EMPTY

				val matchingItems = matchingTag.toList()
				if (matchingItems.isEmpty()) return ItemStack.EMPTY

				val randomItem = matchingItems[random.nextInt(matchingItems.size)].value()

				var stackForItem = displayStacks[randomItem]
				if (stackForItem == null) {
					stackForItem = ItemStack(randomItem)
					val displayName = Component.empty()
						.append(ModMenuLang.FILTER_ITEM_TAG.toComponent())
						.append(": #${tagKey.location()}")
					stackForItem.set(DataComponents.ITEM_NAME, displayName)
					displayStacks[randomItem] = stackForItem
				}

				displayStack = stackForItem
			}

			return displayStack ?: ItemStack.EMPTY
		}

		override fun test(stack: ItemStack): Boolean = stack.isItem(tagKey)
		override fun getType(): Type = Type.TAG

		fun getNextTag(): TagKey<Item> {
			val tags = backupStack.tags.toList()
			if (tags.isEmpty()) return tagKey

			val currentIndex = tags.indexOf(tagKey)

			return tags[(currentIndex + 1) % tags.size]
		}

		fun asSpecificItem(): SpecificItem {
			return SpecificItem(backupStack, ComponentMatch.IGNORE_ALL)
		}

		companion object {
			private val random = Random(123L)

			val CODEC: MapCodec<Tag> =
				RecordCodecBuilder.mapCodec { instance ->
					instance.group(
						TagKey.codec(Registries.ITEM)
							.fieldOf("tag")
							.forGetter(Tag::tagKey),
						ItemStack.CODEC
							.fieldOf("backup_stack")
							.forGetter(Tag::backupStack)
					).apply(instance, ::Tag)
				}

			val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, Tag> =
				StreamCodec.composite(
					AaronExtraStreamCodecs.tagKeyStreamCodec(Registries.ITEM), Tag::tagKey,
					ItemStack.STREAM_CODEC, Tag::backupStack,
					::Tag
				)
		}
	}

	data class SpecificItem(
		val stack: ItemStack,
		val componentMatch: ComponentMatch
	) : ItemFilterEntry {

		override fun getDisplayStack(registries: HolderLookup.Provider): ItemStack = stack

		override fun test(stack: ItemStack): Boolean {
			if (!this.stack.isItem(stack.item)) return false

			return when (componentMatch) {
				ComponentMatch.IGNORE_ALL -> true
				ComponentMatch.IGNORE_DAMAGE -> isSameComponentsWithoutDamage(this.stack, stack)
				ComponentMatch.EXACT -> this.stack.isSameItemSameComponents(stack)
			}
		}

		override fun getType(): Type = Type.ITEM

		fun withNextComponentMatch(): SpecificItem {
			return copy(componentMatch = componentMatch.next())
		}

		companion object {
			val CODEC: MapCodec<SpecificItem> =
				RecordCodecBuilder.mapCodec { instance ->
					instance.group(
						ItemStack.CODEC
							.fieldOf("stack")
							.forGetter(SpecificItem::stack),
						ComponentMatch.CODEC
							.optionalFieldOf("component_match", ComponentMatch.IGNORE_ALL)
							.forGetter(SpecificItem::componentMatch)
					).apply(instance, ::SpecificItem)
				}

			val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, SpecificItem> =
				StreamCodec.composite(
					ItemStack.STREAM_CODEC, SpecificItem::stack,
					ComponentMatch.STREAM_CODEC, SpecificItem::componentMatch,
					::SpecificItem
				)

			private fun isSameComponentsWithoutDamage(leftStack: ItemStack, rightStack: ItemStack): Boolean {
				val leftComponents = componentsWithoutDamage(leftStack)
				val rightComponents = componentsWithoutDamage(rightStack)

				return leftComponents == rightComponents
			}

			private fun componentsWithoutDamage(stack: ItemStack): Map<DataComponentType<*>, Optional<*>> {
				val components = Reference2ObjectOpenHashMap<DataComponentType<*>, Optional<*>>()

				for ((type, value) in stack.componentsPatch.entrySet()) {
					if (type === DataComponents.DAMAGE) continue
					components[type] = value
				}

				return components
			}
		}
	}

	enum class ComponentMatch(private val id: String) : StringRepresentable {
		IGNORE_ALL("ignore_all"),
		IGNORE_DAMAGE("ignore_damage"),
		EXACT("exact");

		override fun getSerializedName(): String = id

		fun next(): ComponentMatch {
			return entries[(ordinal + 1) % entries.size]
		}

		companion object {
			val CODEC: Codec<ComponentMatch> =
				StringRepresentable.fromEnum { entries.toTypedArray() }
			val STREAM_CODEC: StreamCodec<ByteBuf, ComponentMatch> =
				AaronExtraStreamCodecs.enumStreamCodec(ComponentMatch::class.java)
		}
	}

}