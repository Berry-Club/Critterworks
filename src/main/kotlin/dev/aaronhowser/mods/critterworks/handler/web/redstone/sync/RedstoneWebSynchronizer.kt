package dev.aaronhowser.mods.critterworks.handler.web.redstone.sync

import dev.aaronhowser.mods.aaron.packet.AaronPacket
import dev.aaronhowser.mods.critterworks.handler.web.common.line.WebLine
import dev.aaronhowser.mods.critterworks.handler.web.redstone.RedstoneWebLine
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
		sendToTracking(level, lines, packet)
	}

	fun synchronizePower(player: ServerPlayer, lines: Collection<RedstoneWebLine>) {
		if (lines.isEmpty()) return

		SyncRedstoneWebPowerPacket(createPowerData(lines))
			.messagePlayer(player)
	}

	fun sendToTracking(level: ServerLevel, line: WebLine, packet: AaronPacket) {
		sendToTrackingChunks(level, line.intersectedChunkPositions, packet)
	}

	fun sendToTracking(level: ServerLevel, lines: Collection<WebLine>, packet: AaronPacket) {
		val chunkPositions = mutableSetOf<ChunkPos>()

		for (line in lines) {
			chunkPositions.addAll(line.intersectedChunkPositions)
		}

		sendToTrackingChunks(level, chunkPositions, packet)
	}

	fun sendToTracking(level: ServerLevel, arachnode: Arachnode, packet: AaronPacket) {
		val chunkPosition = ChunkPos(BlockPos.containing(arachnode.position))
		packet.messageAllPlayersTrackingChunk(level, chunkPosition)
	}

	fun sendArachnodeRemoval(
		level: ServerLevel,
		arachnode: Arachnode,
		connectedLines: Collection<WebLine>,
		packet: AaronPacket
	) {
		val chunkPositions = mutableSetOf(
			ChunkPos(BlockPos.containing(arachnode.position))
		)

		for (line in connectedLines) {
			chunkPositions += line.intersectedChunkPositions
		}

		sendToTrackingChunks(level, chunkPositions, packet)
	}

	private fun sendToTrackingChunks(
		level: ServerLevel,
		chunkPositions: Set<ChunkPos>,
		packet: AaronPacket
	) {
		if (chunkPositions.isEmpty()) return

		if (chunkPositions.size == 1) {
			packet.messageAllPlayersTrackingChunk(level, chunkPositions.first())
			return
		}

		val players = chunkPositions
			.flatMap { level.chunkSource.chunkMap.getPlayers(it, false) }
			.toSet()

		for (player in players) {
			packet.messagePlayer(player)
		}
	}

	private fun createPowerData(lines: Collection<RedstoneWebLine>): List<SyncRedstoneWebPowerPacket.LinePower> {
		val powerData = mutableListOf<SyncRedstoneWebPowerPacket.LinePower>()

		for (line in lines) {
			powerData += SyncRedstoneWebPowerPacket.LinePower(line.uuid, line.power)
		}

		return powerData
	}
}