package dev.aaronhowser.mods.critterworks.handler.web.redstone

import dev.aaronhowser.mods.aaron.packet.AaronPacket
import dev.aaronhowser.mods.critterworks.handler.web.common.line.WebLine
import dev.aaronhowser.mods.critterworks.handler.web.redstone.node.Arachnode
import dev.aaronhowser.mods.critterworks.packet.server_to_client.SyncRedstoneWebPowerPacket
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.ChunkPos

object RedstoneWebSynchronizer {

	fun synchronizePower(level: ServerLevel, lines: Collection<RedstoneWebLine>) {
		if (lines.isEmpty()) return

		val packet = SyncRedstoneWebPowerPacket(createPowerData(lines))
		for (player in getTrackingPlayers(level, lines)) packet.messagePlayer(player)
	}

	fun synchronizePower(player: ServerPlayer, lines: Collection<RedstoneWebLine>) {
		if (lines.isEmpty()) return
		SyncRedstoneWebPowerPacket(createPowerData(lines)).messagePlayer(player)
	}

	fun sendToTracking(level: ServerLevel, line: WebLine, packet: AaronPacket) {
		for (player in getTrackingPlayers(level, listOf(line))) packet.messagePlayer(player)
	}

	fun sendToTracking(level: ServerLevel, lines: Collection<WebLine>, packet: AaronPacket) {
		for (player in getTrackingPlayers(level, lines)) packet.messagePlayer(player)
	}

	fun sendToTracking(level: ServerLevel, arachnode: Arachnode, packet: AaronPacket) {
		val chunkPosition = ChunkPos(BlockPos.containing(arachnode.position))
		for (player in level.chunkSource.chunkMap.getPlayers(chunkPosition, false)) {
			packet.messagePlayer(player)
		}
	}

	fun getTrackingPlayers(
		level: ServerLevel,
		lines: Collection<WebLine>
	): Set<ServerPlayer> {
		val players: MutableSet<ServerPlayer> = mutableSetOf()
		for (line in lines) {
			for (chunkPosition in line.intersectedChunkPositions) {
				players.addAll(level.chunkSource.chunkMap.getPlayers(chunkPosition, false))
			}
		}

		return players
	}

	private fun createPowerData(lines: Collection<RedstoneWebLine>): List<RedstoneWebPowerData> {
		val powerData: MutableList<RedstoneWebPowerData> = mutableListOf()
		for (line in lines) powerData.add(RedstoneWebPowerData(line.uuid, line.power))
		return powerData
	}
}
