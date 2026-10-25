package dev.aaronhowser.mods.critterworks.handler.snail

import net.minecraft.core.HolderLookup
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.saveddata.SavedData
import net.minecraft.world.phys.Vec3
import java.util.*

class EvilSnailHandler : SavedData() {

	val snails: Map<UUID, EvilSnail>
		field = mutableMapOf()

	fun hasSnail(player: ServerPlayer): Boolean {
		return snails.containsKey(player.uuid)
	}

	fun addSnail(player: ServerPlayer, position: Vec3) {
		snails[player.uuid] = EvilSnail(position, player.level().dimension())
		setDirty()
	}

	fun tick(server: MinecraftServer) {
		var changed = false
		val removedSnails = mutableListOf<UUID>()

		for ((playerUuid, snail) in snails) {
			val player = server.playerList.getPlayer(playerUuid)

			val tickResult = snail.tick(player)
			if (tickResult == EvilSnail.TickResult.CHANGED) {
				changed = true
			} else if (tickResult == EvilSnail.TickResult.REMOVE) {
				removedSnails.add(playerUuid)
				changed = true
				continue
			}

			val level = server.getLevel(snail.dimension) ?: continue
			level.sendParticles(
				ParticleTypes.END_ROD,
				snail.pos.x,
				snail.pos.y,
				snail.pos.z,
				1,
				0.0,
				0.0,
				0.0,
				0.0
			)
		}

		for (playerUuid in removedSnails) {
			snails.remove(playerUuid)
		}

		if (changed) setDirty()
	}

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