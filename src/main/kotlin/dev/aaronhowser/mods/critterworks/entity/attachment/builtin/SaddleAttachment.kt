package dev.aaronhowser.mods.critterworks.entity.attachment.builtin

import dev.aaronhowser.mods.critterworks.entity.ScoochwormPartEntity
import dev.aaronhowser.mods.critterworks.entity.attachment.ItemStackScoochwormAttachment
import dev.aaronhowser.mods.critterworks.entity.attachment.data.SaddleAttachmentData
import dev.aaronhowser.mods.critterworks.entity.attachment.data.SyncedAttachmentData
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack

class SaddleAttachment(
	saddle: ItemStack
) : ItemStackScoochwormAttachment(saddle) {

	override val syncedData: SyncedAttachmentData = SaddleAttachmentData
	override val equipSound: SoundEvent = SoundEvents.HORSE_SADDLE

	override fun interact(
		player: Player,
		heldStack: ItemStack,
		bodyPart: ScoochwormPartEntity
	): InteractionResult {
		if (player.isShiftKeyDown) return InteractionResult.PASS
		if (!player.startRiding(bodyPart)) return InteractionResult.PASS
		return InteractionResult.CONSUME
	}

	override fun predictInteraction(
		player: Player,
		heldStack: ItemStack,
		bodyPart: ScoochwormPartEntity
	): InteractionResult {
		return if (player.isShiftKeyDown) InteractionResult.PASS else InteractionResult.SUCCESS
	}
}