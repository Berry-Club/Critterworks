package dev.aaronhowser.mods.critterworks.handler.snail

import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.saveddata.SavedData
import java.util.*

class EvilSnailHandler : SavedData() {

	val snails: Map<UUID, EvilSnail>
		field = mutableMapOf()

	override fun save(tag: CompoundTag, registries: HolderLookup.Provider): CompoundTag {
		val snailsTag = CompoundTag()

		for ((uuid, snail) in snails) {
			snailsTag.put(uuid.toString(), snail.toTag())
		}

		tag.put(SNAILS_TAG, snailsTag)
		return tag
	}

	companion object {
		const val SAVED_DATA_NAME = "critterworks_evil_snail"
		private const val SNAILS_TAG = "Snails"

		fun get(level: ServerLevel): EvilSnailHandler {
			if (level != level.server.overworld()) {
				return get(level.server.overworld())
			}

			val storage = level.dataStorage
			val factory = Factory(::EvilSnailHandler, ::load)

			return storage.computeIfAbsent(factory, SAVED_DATA_NAME)
		}

		private fun load(tag: CompoundTag, provider: HolderLookup.Provider): EvilSnailHandler {
			val handler = EvilSnailHandler()
			val snailsTag = tag.getCompound(SNAILS_TAG)

			for (uuidString in snailsTag.allKeys) {
				val uuidResult = runCatching {
					UUID.fromString(uuidString)
				}
				val uuid = uuidResult.getOrNull() ?: continue
				val snailTag = snailsTag.getCompound(uuidString)
				handler.snails[uuid] = EvilSnail.fromTag(snailTag)
			}

			return handler
		}
	}

}