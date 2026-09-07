package dev.aaronhowser.mods.critterworks.entity.attachment.builtin

import dev.aaronhowser.mods.critterworks.entity.ScoochwormPartEntity
import dev.aaronhowser.mods.critterworks.entity.attachment.ScoochwormAttachment
import dev.aaronhowser.mods.critterworks.entity.attachment.data.ChunkLoaderAttachmentData
import dev.aaronhowser.mods.critterworks.entity.attachment.data.SyncedAttachmentData
import dev.aaronhowser.mods.critterworks.handler.chunkloader.ChunkLoaderSavedData
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.ChunkPos
import net.neoforged.neoforge.common.util.FakePlayer
import java.util.*

class ChunkLoaderAttachment(
	gps: ItemStack
) : ScoochwormAttachment(gps) {

	override var syncedData: SyncedAttachmentData = ChunkLoaderAttachmentData(
		UUID(0L, 0L),
		UUID(0L, 0L)
	)

	override val equipSound: SoundEvent = SoundEvents.IRON_GOLEM_REPAIR

	override fun install(player: Player): Boolean {
		if (player is FakePlayer) return false

		syncedData = ChunkLoaderAttachmentData(
			UUID.randomUUID(),
			player.uuid
		)

		return true
	}

	override fun save(): CompoundTag {
		val tag = super.save()
		val data = syncedData as? ChunkLoaderAttachmentData ?: return tag

		tag.putUUID(UUID_TAG, data.uuid)
		tag.putUUID(PLACER_UUID_TAG, data.placerUuid)

		return tag
	}

	override fun load(tag: CompoundTag) {
		syncedData = ChunkLoaderAttachmentData(
			tag.getUUID(UUID_TAG),
			tag.getUUID(PLACER_UUID_TAG)
		)
	}

	override fun applySyncedData(data: SyncedAttachmentData) {
		if (data is ChunkLoaderAttachmentData) {
			syncedData = data
		}
	}

	override fun serverTick(bodyPart: ScoochwormPartEntity) {
		val head = bodyPart.getScoochworm() ?: return
		val data = syncedData as? ChunkLoaderAttachmentData ?: return
		val level = head.level() as? ServerLevel ?: return

		ChunkLoaderSavedData.get(level)
			.updateRecord(
				data.uuid,
				data.placerUuid,
				head.uuid,
				ChunkPos(head.blockPosition())
			)
	}

	override fun onRemoved(bodyPart: ScoochwormPartEntity) {
		val data = syncedData as? ChunkLoaderAttachmentData ?: return
		val level = bodyPart.level() as? ServerLevel ?: return
		ChunkLoaderSavedData.get(level).removeRecord(data.uuid)
	}

	companion object {
		private const val UUID_TAG = "Uuid"
		private const val PLACER_UUID_TAG = "PlacerUuid"
	}
}