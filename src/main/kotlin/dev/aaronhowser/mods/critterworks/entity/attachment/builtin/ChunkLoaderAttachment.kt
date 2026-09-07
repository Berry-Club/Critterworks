package dev.aaronhowser.mods.critterworks.entity.attachment.builtin

import dev.aaronhowser.mods.critterworks.entity.ScoochwormPartEntity
import dev.aaronhowser.mods.critterworks.entity.attachment.ScoochwormAttachment
import dev.aaronhowser.mods.critterworks.entity.attachment.data.ChunkLoaderAttachmentData
import dev.aaronhowser.mods.critterworks.entity.attachment.data.SyncedAttachmentData
import dev.aaronhowser.mods.critterworks.config.ServerConfig
import dev.aaronhowser.mods.critterworks.handler.chunkloader.ChunkLoaderSavedData
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.TicketType
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

	private var loadedChunk: ChunkPos = ChunkPos.ZERO
	private var loadedLevel: ServerLevel? = null

	override fun install(player: Player): Boolean {
		if (player is FakePlayer) return false

		val level = player.level() as? ServerLevel ?: return false

		val loaderCount = ChunkLoaderSavedData
			.getAllRecords(level.server, player.uuid)
			.size

		val maxLoaders = ServerConfig.CONFIG.maxChunkLoadersPerPlayer.get()
		if (loaderCount >= maxLoaders) return false

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
		val oldLevel = loadedLevel

		val newChunk = ChunkPos(head.blockPosition())
		val oldChunk = loadedChunk
		if (newChunk == oldChunk && level === oldLevel) return

		if (oldLevel != null) {
			removeTickets(oldLevel, oldChunk, data.uuid)
		}

		addTickets(level, newChunk, data.uuid)
		loadedChunk = newChunk
		loadedLevel = level

		ChunkLoaderSavedData.get(level)
			.updateRecord(
				data.uuid,
				data.placerUuid,
				head.uuid,
				newChunk
			)
	}

	override fun onRemoved(bodyPart: ScoochwormPartEntity) {
		val data = syncedData as? ChunkLoaderAttachmentData ?: return
		val level = bodyPart.level() as? ServerLevel ?: return
		removeTickets(loadedLevel ?: level, loadedChunk, data.uuid)
		ChunkLoaderSavedData.get(level).removeRecord(data.uuid)
	}

	companion object {
		private val TICKET_TYPE: TicketType<UUID> = TicketType.create(
			"critterworks_chunk_loader",
			Comparator.comparing(UUID::toString)
		)

		private const val UUID_TAG = "Uuid"
		private const val PLACER_UUID_TAG = "PlacerUuid"

		private fun addTickets(level: ServerLevel, center: ChunkPos, attachmentUuid: UUID) {
			val chunkSource = level.chunkSource

			val chunks = getChunkShape(center)
			for (chunk in chunks) {
				chunkSource.addRegionTicket(TICKET_TYPE, chunk, 0, attachmentUuid)
			}
		}

		private fun removeTickets(level: ServerLevel, center: ChunkPos, attachmentUuid: UUID) {
			val chunkSource = level.chunkSource

			val chunks = getChunkShape(center)
			for (chunk in chunks) {
				chunkSource.removeRegionTicket(TICKET_TYPE, chunk, 0, attachmentUuid)
			}
		}

		private fun getChunkShape(center: ChunkPos): List<ChunkPos> {
			return listOf(
				center,
				ChunkPos(center.x + 1, center.z),
				ChunkPos(center.x - 1, center.z),
				ChunkPos(center.x, center.z + 1),
				ChunkPos(center.x, center.z - 1),
			)
		}
	}
}