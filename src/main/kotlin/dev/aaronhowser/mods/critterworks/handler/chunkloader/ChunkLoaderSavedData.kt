package dev.aaronhowser.mods.critterworks.handler.chunkloader

import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.saveddata.SavedData
import java.util.*

class ChunkLoaderSavedData : SavedData() {

	private val recordsByAttachment: MutableMap<UUID, ChunkLoaderRecord> = mutableMapOf()

	fun updateRecord(attachmentUuid: UUID, placerUuid: UUID, headUuid: UUID, headChunk: ChunkPos) {
		val record = ChunkLoaderRecord(attachmentUuid, placerUuid, headUuid, headChunk)
		if (recordsByAttachment[attachmentUuid] == record) return

		recordsByAttachment[attachmentUuid] = record
		setDirty()
	}

	fun removeRecord(attachmentUuid: UUID) {
		if (recordsByAttachment.remove(attachmentUuid) == null) return

		setDirty()
	}

	fun getRecords(): Collection<ChunkLoaderRecord> = recordsByAttachment.values

	fun getRecordsForPlayer(placerUuid: UUID): Collection<ChunkLoaderRecord> {
		return recordsByAttachment.values.filter { it.placerUuid == placerUuid }
	}

	fun getPlayerAttachmentCount(placerUuid: UUID): Int {
		return getRecordsForPlayer(placerUuid).size
	}

	override fun save(tag: CompoundTag, registries: HolderLookup.Provider): CompoundTag {
		val attachmentsTag = CompoundTag()

		for ((attachmentUuid, record) in recordsByAttachment) {
			val chunkTag = CompoundTag()
			chunkTag.putUUID(PLACER_UUID_TAG, record.placerUuid)
			chunkTag.putUUID(HEAD_UUID_TAG, record.headUuid)
			chunkTag.putInt(X_TAG, record.headChunk.x)
			chunkTag.putInt(Z_TAG, record.headChunk.z)
			attachmentsTag.put(attachmentUuid.toString(), chunkTag)
		}

		tag.put(ATTACHMENTS_TAG, attachmentsTag)
		return tag
	}

	companion object {
		private const val SAVED_DATA_NAME = "critterworks_chunk_loaders"
		private const val ATTACHMENTS_TAG = "Attachments"
		private const val X_TAG = "X"
		private const val Z_TAG = "Z"
		private const val PLACER_UUID_TAG = "PlacerUuid"
		private const val HEAD_UUID_TAG = "HeadUuid"

		private fun load(tag: CompoundTag, registries: HolderLookup.Provider): ChunkLoaderSavedData {
			val savedData = ChunkLoaderSavedData()
			val attachmentsTag = tag.getCompound(ATTACHMENTS_TAG)

			for (attachmentUuidString in attachmentsTag.allKeys) {
				val result = runCatching {
					UUID.fromString(attachmentUuidString)
				}

				val attachmentUuid = result.getOrNull() ?: continue
				val chunkTag = attachmentsTag.getCompound(attachmentUuidString)

				val x = chunkTag.getInt(X_TAG)
				val z = chunkTag.getInt(Z_TAG)

				if (!chunkTag.hasUUID(PLACER_UUID_TAG) || !chunkTag.hasUUID(HEAD_UUID_TAG)) continue

				savedData.recordsByAttachment[attachmentUuid] = ChunkLoaderRecord(
					attachmentUuid,
					chunkTag.getUUID(PLACER_UUID_TAG),
					chunkTag.getUUID(HEAD_UUID_TAG),
					ChunkPos(x, z)
				)
			}

			return savedData
		}

		fun get(level: ServerLevel): ChunkLoaderSavedData {
			return level.dataStorage
				.computeIfAbsent(
					Factory(
						::ChunkLoaderSavedData,
						::load
					),
					SAVED_DATA_NAME
				)
		}
	}
}