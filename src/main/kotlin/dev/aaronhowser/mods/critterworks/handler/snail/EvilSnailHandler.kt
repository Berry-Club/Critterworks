package dev.aaronhowser.mods.critterworks.handler.snail

import dev.aaronhowser.mods.critterworks.packet.server_to_client.SyncEvilSnailsPacket
import dev.aaronhowser.mods.critterworks.packet.server_to_client.SyncEvilSnailsPacket.SnailData
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.saveddata.SavedData
import net.minecraft.world.phys.Vec3
import java.util.*
import kotlin.math.atan2

class EvilSnailHandler : SavedData() {

	val snails: Map<UUID, EvilSnail>
		field = mutableMapOf()

	fun hasSnail(entity: Entity): Boolean {
		return snails.containsKey(entity.uuid)
	}

	fun addSnail(entity: Entity, position: Vec3) {
		snails[entity.uuid] = EvilSnail(position, entity.level().dimension())
		setDirty()
	}

	fun removeSnail(entity: Entity) {
		snails.remove(entity.uuid)
		setDirty()
	}

	fun getSnail(entity: Entity): EvilSnail? {
		return snails[entity.uuid]
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
		}

		for (playerUuid in removedSnails) {
			snails.remove(playerUuid)
		}

		if (changed) setDirty()

		syncSnails(server)
	}

	private fun syncSnails(server: MinecraftServer) {
		for (player in server.playerList.players) {
			val nearbySnails = mutableListOf<SnailData>()

			for ((snailUuid, snail) in snails) {
				if (snail.dimension != player.level().dimension()) continue
				if (snail.pos.distanceToSqr(player.position()) > SYNC_DISTANCE_SQUARED) continue

				val chasedPlayer = server.playerList.getPlayer(snailUuid)
				val yRot = getYRot(snail, chasedPlayer)

				nearbySnails.add(
					SnailData(
						snailUuid,
						snail.pos,
						yRot
					)
				)

				if (nearbySnails.size >= SyncEvilSnailsPacket.MAX_SNAILS) break
			}

			SyncEvilSnailsPacket(nearbySnails).messagePlayer(player)
		}
	}

	private fun getYRot(snail: EvilSnail, chasedPlayer: ServerPlayer?): Float {
		if (chasedPlayer == null) return 0f
		if (snail.dimension != chasedPlayer.level().dimension()) return 0f

		val target = chasedPlayer.position().lerp(chasedPlayer.eyePosition, 0.5)
		val offset = snail.pos.vectorTo(target)
		return Mth.wrapDegrees(
			Math.toDegrees(atan2(-offset.x, offset.z))
		).toFloat()
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
		private const val SYNC_DISTANCE = 128.0
		private const val SYNC_DISTANCE_SQUARED = SYNC_DISTANCE * SYNC_DISTANCE

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