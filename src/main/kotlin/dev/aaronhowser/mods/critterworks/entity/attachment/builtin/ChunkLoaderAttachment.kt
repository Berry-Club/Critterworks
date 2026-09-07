package dev.aaronhowser.mods.critterworks.entity.attachment.builtin

import dev.aaronhowser.mods.critterworks.entity.attachment.ScoochwormAttachment
import dev.aaronhowser.mods.critterworks.entity.attachment.data.ChunkLoaderAttachmentData
import dev.aaronhowser.mods.critterworks.entity.attachment.data.SyncedAttachmentData
import net.minecraft.nbt.CompoundTag
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.common.util.FakePlayer
import java.util.UUID

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
		if (data is ChunkLoaderAttachmentData) syncedData = data
	}

	companion object {
		private const val UUID_TAG = "Uuid"
		private const val PLACER_UUID_TAG = "PlacerUuid"
	}
}