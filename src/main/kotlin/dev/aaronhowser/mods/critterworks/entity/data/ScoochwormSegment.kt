package dev.aaronhowser.mods.critterworks.entity.data

import dev.aaronhowser.mods.aaron.misc.AaronExtensions.getEquipmentSlot
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isItem
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.nextRange
import dev.aaronhowser.mods.critterworks.advancement.ModAdvancements
import dev.aaronhowser.mods.critterworks.block.base.ScoochwormSegmentSupportBlock
import dev.aaronhowser.mods.critterworks.entity.ScoochwormEntity
import dev.aaronhowser.mods.critterworks.entity.ScoochwormPartEntity
import dev.aaronhowser.mods.critterworks.entity.attachment.ScoochwormAttachment
import dev.aaronhowser.mods.critterworks.entity.attachment.builtin.LockboxAttachment
import dev.aaronhowser.mods.critterworks.entity.attachment.builtin.NoAttachment
import dev.aaronhowser.mods.critterworks.entity.attachment.data.SyncedAttachmentData
import dev.aaronhowser.mods.critterworks.registry.ModEntityTypes
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.gameevent.GameEvent
import net.minecraft.world.phys.Vec3
import net.neoforged.neoforge.items.IItemHandler

// The segment is the actual thing that gets saved to the head entity
// It holds the attachment etc
class ScoochwormSegment {

	private var attachment: ScoochwormAttachment = NoAttachment()
	private var previousSupportPosition: BlockPos? = null
	private var previousSupportState: BlockState? = null

	// The segment owns the entity, not the other way around
	// The entity doesn't even save anything, actually
	var bodyPart: ScoochwormPartEntity? = null
		private set

	fun updateBodyPart(
		scoochworm: ScoochwormEntity,
		partIndex: Int,
		pathPoint: ScoochwormPathPoint
	) {
		var bodyPart = this.bodyPart

		if (bodyPart == null || bodyPart.isRemoved) {
			bodyPart = createBodyPart(scoochworm, partIndex, pathPoint.position)
			this.bodyPart = bodyPart
		}

		bodyPart.moveAlongPath(pathPoint.position, pathPoint.supportDirection)
	}

	fun discardBodyPart() {
		val existingBodyPart = bodyPart
		if (existingBodyPart != null) {
			val level = existingBodyPart.level()
			if (level is ServerLevel) {
				notifyDetached(
					level,
					previousSupportPosition,
					previousSupportState,
					existingBodyPart
				)
			}
		}

		bodyPart?.discard()
		bodyPart = null
		previousSupportPosition = null
		previousSupportState = null
	}

	fun bindClientBodyPart(
		bodyPart: ScoochwormPartEntity,
		attachmentData: SyncedAttachmentData
	) {
		this.bodyPart = bodyPart

		if (attachment.syncedData.typeId != attachmentData.typeId) {
			attachment = ScoochwormAttachment.createClient(attachmentData)
		} else {
			attachment.applySyncedData(attachmentData)
		}
	}

	fun unbindClientBodyPart(bodyPart: ScoochwormPartEntity) {
		if (this.bodyPart === bodyPart) {
			this.bodyPart = null
		}
	}

	fun reparentBodyPart(scoochworm: ScoochwormEntity, partIndex: Int) {
		bodyPart?.attachTo(scoochworm, partIndex, attachment.syncedData, this)
	}

	private fun installAttachment(
		itemStack: ItemStack,
		player: Player,
		bodyPart: ScoochwormPartEntity
	): Boolean {
		val attachmentItem = itemStack.copy()
		attachmentItem.count = 1

		val newAttachment = ScoochwormAttachment.fromItemStack(attachmentItem)
		if (newAttachment is NoAttachment) return false
		if (!newAttachment.install(player)) return false

		attachment = newAttachment
		bodyPart.attachmentData = attachment.syncedData

		ModAdvancements.trigger(player, ModAdvancements.ATTACH_TO_SCOOCHWORM)

		val equipSound = attachment.equipSound
		if (equipSound != null) {
			bodyPart.playSound(
				equipSound,
				1f,
				bodyPart.random.nextRange(0.8f, 1.2f)
			)
		}
		bodyPart.gameEvent(GameEvent.EQUIP, player)

		return true
	}

	private fun removeAttachment(): ItemStack {
		val removedItem = attachment.remove()
		attachment = NoAttachment()
		bodyPart?.attachmentData = attachment.syncedData
		return removedItem
	}

	fun interact(
		player: Player,
		hand: InteractionHand,
		heldStack: ItemStack,
		onSheared: () -> Unit
	): InteractionResult {
		val bodyPart = bodyPart ?: return InteractionResult.PASS

		if (heldStack.isItem(Items.SHEARS)) {
			bodyPart.playSound(SoundEvents.SHEEP_SHEAR, 1f, 1f)
			bodyPart.gameEvent(GameEvent.SHEAR, player)
			onSheared()

			ModAdvancements.trigger(player, ModAdvancements.SPLIT_SCOOCHWORM)

			val equipmentSlot = hand.getEquipmentSlot()
			heldStack.hurtAndBreak(1, player, equipmentSlot)
			return InteractionResult.CONSUME
		}

		if (
			attachment !is NoAttachment
			&& player.isShiftKeyDown
			&& heldStack.isEmpty
		) {
			val attachmentItem = removeAttachment()

			if (!player.addItem(attachmentItem)) {
				player.drop(attachmentItem, false)
			}

			bodyPart.playSound(
				SoundEvents.ITEM_FRAME_REMOVE_ITEM,
				1f,
				bodyPart.random.nextRange(0.8f, 1.2f)
			)
			bodyPart.gameEvent(GameEvent.UNEQUIP, player)
			return InteractionResult.CONSUME
		}

		if (attachment is NoAttachment) {
			if (!installAttachment(heldStack, player, bodyPart)) return InteractionResult.PASS

			heldStack.consume(1, player)
			return InteractionResult.CONSUME
		}

		return attachment.interact(player, heldStack, bodyPart)
	}

	fun dropAttachmentItem(entity: Entity) {
		val attachmentItem = removeAttachment()
		if (attachmentItem.isEmpty) return

		val dropSource = bodyPart ?: entity
		dropSource.spawnAtLocation(attachmentItem)
	}

	fun getItemHandler(): IItemHandler? {
		return attachment.itemHandler
	}

	fun getAttachment(): ScoochwormAttachment {
		return attachment
	}

	fun insertIntoLockbox(itemStack: ItemStack): ItemStack {
		val lockbox = attachment as? LockboxAttachment ?: return itemStack
		return lockbox.insert(itemStack)
	}

	fun serverTick() {
		val bodyPart = bodyPart ?: return
		attachment.serverTick(bodyPart)
		updateSupportBlock(bodyPart)
	}

	private fun updateSupportBlock(bodyPart: ScoochwormPartEntity) {
		val level = bodyPart.level() as? ServerLevel ?: return
		val supportPosition = ScoochwormEntity.getSupportBlockPosition(
			bodyPart.position(),
			bodyPart.supportDirection
		)

		val supportState = level.getBlockState(supportPosition)
		val supportBlock = supportState.block as? ScoochwormSegmentSupportBlock

		val listenerPosition = if (supportBlock != null) {
			supportPosition
		} else {
			null
		}

		val supportChanged = previousSupportPosition != listenerPosition
			|| previousSupportState?.block !== supportState.block

		if (supportChanged) {
			notifyDetached(
				level,
				previousSupportPosition,
				previousSupportState,
				bodyPart
			)

			supportBlock?.onSegmentAttached(supportState, level, supportPosition, bodyPart)
		}

		supportBlock?.onSegmentTick(supportState, level, supportPosition, bodyPart)

		previousSupportPosition = listenerPosition
		previousSupportState = if (supportBlock != null) supportState else null
	}

	private fun notifyDetached(
		level: ServerLevel,
		position: BlockPos?,
		state: BlockState?,
		bodyPart: ScoochwormPartEntity
	) {
		if (position == null || state == null) return
		val supportBlock = state.block as? ScoochwormSegmentSupportBlock ?: return

		supportBlock.onSegmentDetached(state, level, position, bodyPart)
	}

	fun clientTick() {
		val bodyPart = bodyPart ?: return
		attachment.clientTick(bodyPart)
	}

	private fun createBodyPart(
		scoochworm: ScoochwormEntity,
		partIndex: Int,
		position: Vec3
	): ScoochwormPartEntity {
		val bodyPart = ScoochwormPartEntity(
			ModEntityTypes.SCOOCHWORM_PART.get(),
			scoochworm.level()
		)

		bodyPart.attachTo(
			scoochworm,
			partIndex,
			attachment.syncedData,
			this
		)

		bodyPart.moveTo(
			position.x,
			position.y,
			position.z,
			scoochworm.yRot,
			scoochworm.xRot
		)

		scoochworm.level().addFreshEntity(bodyPart)
		return bodyPart
	}

	fun save(): CompoundTag {
		return attachment.save()
	}

	fun predictInteraction(player: Player, heldStack: ItemStack): InteractionResult {
		if (heldStack.isItem(Items.SHEARS)) return InteractionResult.SUCCESS
		if (attachment is NoAttachment) {
			return if (ScoochwormAttachment.canAttach(heldStack)) {
				InteractionResult.SUCCESS
			} else {
				InteractionResult.PASS
			}
		}

		if (
			player.isShiftKeyDown
			&& heldStack.isEmpty
		) {
			return InteractionResult.SUCCESS
		}

		val bodyPart = bodyPart ?: return InteractionResult.PASS
		return attachment.predictInteraction(player, heldStack, bodyPart)
	}

	companion object {
		fun load(tag: CompoundTag): ScoochwormSegment {
			val segment = ScoochwormSegment()
			segment.attachment = ScoochwormAttachment.load(tag)
			return segment
		}
	}
}
