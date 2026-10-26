package dev.aaronhowser.mods.critterworks.handler.snail

import dev.aaronhowser.mods.critterworks.packet.server_to_client.SyncEvilSnailsPacket.SnailData
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3
import java.util.UUID

object ClientEvilSnails {

	private val snails: MutableMap<UUID, ClientEvilSnail> = mutableMapOf()

	fun getSnails(): Collection<ClientEvilSnail> {
		return snails.values
	}

	fun getSnail(uuid: UUID): ClientEvilSnail? {
		return snails[uuid]
	}

	fun replaceWith(syncedSnails: List<SnailData>) {
		val retainedSnailUuids = mutableSetOf<UUID>()
		val updateTimeNanos = System.nanoTime()

		for (syncedSnail in syncedSnails) {
			retainedSnailUuids.add(syncedSnail.uuid)

			val snail = snails[syncedSnail.uuid]
			if (snail == null) {
				snails[syncedSnail.uuid] = ClientEvilSnail(syncedSnail, updateTimeNanos)
			} else {
				snail.update(syncedSnail, updateTimeNanos)
			}
		}

		snails.keys.retainAll(retainedSnailUuids)
	}

	fun clear() {
		snails.clear()
	}

	class ClientEvilSnail(syncData: SnailData, updateTimeNanos: Long) {

		private var previousPosition: Vec3 = syncData.position
		private var position: Vec3 = syncData.position
		private var previousYRot: Float = syncData.yRot
		private var yRot: Float = syncData.yRot
		private var lastUpdateTimeNanos: Long = updateTimeNanos

		fun update(syncData: SnailData, updateTimeNanos: Long) {
			previousPosition = getPosition(updateTimeNanos)
			previousYRot = getYRot(updateTimeNanos)
			position = syncData.position
			yRot = syncData.yRot
			lastUpdateTimeNanos = updateTimeNanos
		}

		fun getPosition(renderTimeNanos: Long): Vec3 {
			val progress = getInterpolationProgress(renderTimeNanos)
			return previousPosition.lerp(position, progress.toDouble())
		}

		fun getYRot(renderTimeNanos: Long): Float {
			val progress = getInterpolationProgress(renderTimeNanos)
			return Mth.rotLerp(progress, previousYRot, yRot)
		}

		private fun getInterpolationProgress(renderTimeNanos: Long): Float {
			val elapsedNanos = renderTimeNanos - lastUpdateTimeNanos
			return (elapsedNanos.toDouble() / INTERPOLATION_DURATION_NANOS)
				.coerceIn(0.0, 1.0)
				.toFloat()
		}
	}

	private const val INTERPOLATION_DURATION_NANOS = 50_000_000.0

}