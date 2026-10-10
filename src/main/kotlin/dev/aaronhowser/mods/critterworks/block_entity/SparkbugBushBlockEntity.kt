package dev.aaronhowser.mods.critterworks.block_entity

import dev.aaronhowser.mods.aaron.block_entity.SyncingBlockEntity
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.chance
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.randomPos
import dev.aaronhowser.mods.critterworks.config.ClientConfig
import dev.aaronhowser.mods.critterworks.config.ServerConfig
import dev.aaronhowser.mods.critterworks.handler.sparkbug.DisplayPollenPoint
import dev.aaronhowser.mods.critterworks.handler.sparkbug.PollenPointHandler
import dev.aaronhowser.mods.critterworks.registry.ModBlockEntityTypes
import dev.aaronhowser.mods.critterworks.registry.ModParticleTypes
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.Connection
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import kotlin.math.abs

class SparkbugBushBlockEntity(
	pos: BlockPos,
	blockState: BlockState
) : SyncingBlockEntity(ModBlockEntityTypes.SPARKBUG_BUSH.get(), pos, blockState) {

	override val syncImmediately: Boolean = true

	private val pollenPointHandler = PollenPointHandler()

	val displayPollenPoints: Set<DisplayPollenPoint>
		get() = pollenPointHandler.displayPollenPoints

	fun addPollenPoint(
		pos: BlockPos,
		direction: Direction,
		isInput: Boolean
	): Boolean {
		val level = level ?: return false
		val success = pollenPointHandler.addPollenPoint(level, pos, direction, isInput)
		if (!success) return false

		setChanged()
		return true
	}

	fun removePollenPoint(
		pos: BlockPos,
		direction: Direction
	): Boolean {
		val success = pollenPointHandler.removePollenPoint(pos, direction)
		if (!success) return false

		setChanged()
		return true
	}

	private fun serverTick(level: ServerLevel) {
		val maximumTransfer = ServerConfig.CONFIG.sparkbugBushMaxEnergyTransferPerInput.get()
		val displayChanged = pollenPointHandler.serverTick(level, maximumTransfer)
		if (displayChanged) setChanged()
	}

	private fun clientTick(level: Level) {
		pollenPointHandler.clientTick()
		spawnActivePollenParticles(level)
	}

	private fun spawnActivePollenParticles(level: Level) {
		val spawnChance = ClientConfig.CONFIG.fireflyParticleSpawnChance.get()
		val spawnRadius = ClientConfig.CONFIG.fireflyParticleSpawnRadius.get()

		for (pollenSpot in displayPollenPoints) {
			if (!pollenSpot.isActive) continue
			if (!level.random.chance(spawnChance)) continue

			val direction = pollenSpot.direction
			val faceCenter = pollenSpot.pos.center.relative(direction, 0.5)
			val outwardEdge = faceCenter.relative(direction, spawnRadius)

			val spawnArea = AABB(faceCenter, outwardEdge)
				.inflate(
					spawnRadius * (1 - abs(direction.stepX)),
					spawnRadius * (1 - abs(direction.stepY)),
					spawnRadius * (1 - abs(direction.stepZ))
				)

			val particlePosition = spawnArea.randomPos(level.random)

			level.addParticle(
				ModParticleTypes.FIREFLY.get(),
				particlePosition.x,
				particlePosition.y,
				particlePosition.z,
				0.0,
				0.0,
				0.0
			)
		}
	}

	override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
		super.saveAdditional(tag, registries)
		pollenPointHandler.savePersistentData(tag)
	}

	override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
		super.loadAdditional(tag, registries)
		pollenPointHandler.loadPersistentData(tag)
	}

	override fun getUpdateTag(pRegistries: HolderLookup.Provider): CompoundTag {
		val tag = saveWithoutMetadata(pRegistries)
		pollenPointHandler.removePersistentData(tag)
		pollenPointHandler.saveDisplayData(tag)
		return tag
	}

	override fun handleUpdateTag(tag: CompoundTag, lookupProvider: HolderLookup.Provider) {
		super.handleUpdateTag(tag, lookupProvider)
		pollenPointHandler.loadDisplayData(tag)
	}

	override fun onDataPacket(
		connection: Connection,
		packet: ClientboundBlockEntityDataPacket,
		lookupProvider: HolderLookup.Provider
	) {
		handleUpdateTag(packet.tag, lookupProvider)
	}

	companion object {
		fun tick(
			level: Level,
			blockPos: BlockPos,
			blockState: BlockState,
			blockEntity: SparkbugBushBlockEntity
		) {
			if (level is ServerLevel) {
				blockEntity.serverTick(level)
			} else {
				blockEntity.clientTick(level)
			}
		}
	}

}