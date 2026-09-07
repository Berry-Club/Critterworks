package dev.aaronhowser.mods.critterworks.entity.attachment

import dev.aaronhowser.mods.critterworks.entity.ScoochwormPartEntity
import dev.aaronhowser.mods.critterworks.entity.attachment.builtin.NoAttachment
import dev.aaronhowser.mods.critterworks.entity.attachment.data.SyncedAttachmentData
import dev.aaronhowser.mods.critterworks.registry.ModScoochwormAttachmentTypes
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtOps
import net.minecraft.sounds.SoundEvent
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.items.IItemHandler

abstract class ScoochwormAttachment(
	itemStack: ItemStack
) {

	protected val itemStack: ItemStack = itemStack.copy()

	abstract val syncedData: SyncedAttachmentData
	abstract val equipSound: SoundEvent?
	open val itemHandler: IItemHandler? = null

	open fun interact(
		player: Player,
		heldStack: ItemStack,
		bodyPart: ScoochwormPartEntity
	): InteractionResult = InteractionResult.PASS

	open fun predictInteraction(
		player: Player,
		heldStack: ItemStack,
		bodyPart: ScoochwormPartEntity
	): InteractionResult = InteractionResult.SUCCESS

	open fun clientTick(bodyPart: ScoochwormPartEntity) {}

	open fun serverTick(bodyPart: ScoochwormPartEntity) {}

	open fun onRemoved(bodyPart: ScoochwormPartEntity) {}

	open fun install(player: Player): Boolean = true

	open fun applySyncedData(data: SyncedAttachmentData) {}

	protected open fun synchronizeItemStack() {}

	open fun save(): CompoundTag = CompoundTag().apply {
		putString(ATTACHMENT_TYPE_TAG, syncedData.typeId.toString())
	}

	open fun load(tag: CompoundTag) {}

	protected fun saveItemStack(tag: CompoundTag) {
		synchronizeItemStack()
		val encodedTag = ItemStack.OPTIONAL_CODEC.encodeStart(
			NbtOps.INSTANCE,
			itemStack
		)
		encodedTag.result().ifPresent { tag.put(ATTACHMENT_ITEM_TAG, it) }
	}

	open fun remove(): ItemStack {
		synchronizeItemStack()
		return itemStack
	}

	companion object {
		private const val ATTACHMENT_ITEM_TAG = "AttachmentItem"
		private const val ATTACHMENT_TYPE_TAG = "Type"

		fun fromItemStack(
			itemStack: ItemStack
		): ScoochwormAttachment {
			for (type in ModScoochwormAttachmentTypes.REGISTRY) {
				val attachment = type.create(itemStack)
				if (attachment != null) return attachment
			}

			return NoAttachment()
		}

		fun canAttach(itemStack: ItemStack): Boolean {
			for (type in ModScoochwormAttachmentTypes.REGISTRY) {
				if (type.matches(itemStack)) return true
			}

			return false
		}

		fun createClient(data: SyncedAttachmentData): ScoochwormAttachment {
			return data.resolveType().createClientAttachment(data)
		}

		fun load(tag: CompoundTag): ScoochwormAttachment {
			val typeId = net.minecraft.resources.ResourceLocation.parse(tag.getString(ATTACHMENT_TYPE_TAG))
			val type = ModScoochwormAttachmentTypes.REGISTRY.get(typeId)
				?: return NoAttachment()
			val attachment = type.createEmptyAttachment()
			attachment.load(tag)
			return attachment
		}
	}
}