package dev.aaronhowser.mods.critterworks.entity.attachment

import dev.aaronhowser.mods.critterworks.entity.ScoochwormPartEntity
import dev.aaronhowser.mods.critterworks.entity.attachment.builtin.NoAttachment
import dev.aaronhowser.mods.critterworks.entity.attachment.data.SyncedAttachmentData
import dev.aaronhowser.mods.critterworks.entity.data.ScoochwormSegment
import dev.aaronhowser.mods.critterworks.registry.ModScoochwormAttachmentTypes
import net.minecraft.nbt.CompoundTag
import net.minecraft.sounds.SoundEvent
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.items.IItemHandler

abstract class ScoochwormAttachment {
	abstract val syncedData: SyncedAttachmentData
	val type: ScoochwormAttachmentType<*>
		get() = syncedData.resolveType()

	abstract val equipSound: SoundEvent?
	open val itemHandler: IItemHandler? = null
	open val consumesItemStack: Boolean = false

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

	open fun install(player: Player, segment: ScoochwormSegment): Boolean = true

	open fun applySyncedData(data: SyncedAttachmentData) {}

	open fun save(): CompoundTag = CompoundTag().apply {
		putString(ATTACHMENT_TYPE_TAG, syncedData.typeId.toString())
	}

	open fun load(tag: CompoundTag) {}

	open fun remove(): ItemStack {
		return ItemStack.EMPTY
	}

	companion object {
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