package dev.aaronhowser.mods.critterworks.block_entity

import dev.aaronhowser.mods.aaron.block_entity.SyncingBlockEntity
import dev.aaronhowser.mods.critterworks.config.ClientConfig
import dev.aaronhowser.mods.critterworks.config.ServerConfig
import dev.aaronhowser.mods.critterworks.handler.sparkbug.DisplayPollenSpot
import dev.aaronhowser.mods.critterworks.handler.sparkbug.PollenSpotHandler
import dev.aaronhowser.mods.critterworks.registry.ModBlockEntityTypes
import dev.aaronhowser.mods.critterworks.registry.ModParticleTypes
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.Connection
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.RandomSource
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3

class SparkbugBushBlockEntity(
	pos: BlockPos,
	blockState: BlockState
) : SyncingBlockEntity(ModBlockEntityTypes.SPARKBUG_BUSH.get(), pos, blockState) {

	override val syncImmediately: Boolean = true

	private val pollenSpotHandler = PollenSpotHandler()

	val displayPollenSpots: Set<DisplayPollenSpot>
		get() = pollenSpotHandler.displayPollenSpots

	fun addPollenSpot(
		pos: BlockPos,
		direction: Direction,
		isInput: Boolean
	): Boolean {
		val level = level ?: return false
		val success = pollenSpotHandler.addPollenSpot(level, pos, direction, isInput)
		if (!success) return false

		setChanged()
		return true
	}

	fun removePollenSpot(
		pos: BlockPos,
		direction: Direction
	): Boolean {
		val success = pollenSpotHandler.removePollenSpot(pos, direction)
		if (!success) return false

		setChanged()
		return true
	}

	private fun serverTick(level: ServerLevel) {
		val maximumTransfer = ServerConfig.CONFIG.sparkbugBushMaxEnergyTransferPerInput.get()
		val displayChanged = pollenSpotHandler.serverTick(level, maximumTransfer)
		if (displayChanged) setChanged()
	}

	private fun clientTick(level: Level) {
		pollenSpotHandler.clientTick()
		spawnActivePollenParticles(level)
	}

	private fun spawnActivePollenParticles(level: Level) {
		val spawnChance = ClientConfig.CONFIG.fireflyParticleSpawnChance.get()
		val spawnRadius = ClientConfig.CONFIG.fireflyParticleSpawnRadius.get()

		for (pollenSpot in displayPollenSpots) {
			if (!pollenSpot.isActive) continue
			if (level.random.nextDouble() > spawnChance) continue

			val faceCenter = pollenSpot.pos.center
				.add(
					pollenSpot.direction.stepX * 0.5,
					pollenSpot.direction.stepY * 0.5,
					pollenSpot.direction.stepZ * 0.5
				)
			val particlePosition = getParticlePosition(
				level.random,
				faceCenter,
				pollenSpot.direction,
				spawnRadius
			)

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

	private fun getParticlePosition(
		random: RandomSource,
		faceCenter: Vec3,
		direction: Direction,
		spawnRadius: Double
	): Vec3 {
		val xOffset = getParticleOffset(random, direction, Direction.Axis.X, spawnRadius)
		val yOffset = getParticleOffset(random, direction, Direction.Axis.Y, spawnRadius)
		val zOffset = getParticleOffset(random, direction, Direction.Axis.Z, spawnRadius)
		return faceCenter.add(xOffset, yOffset, zOffset)
	}

	private fun getParticleOffset(
		random: RandomSource,
		direction: Direction,
		axis: Direction.Axis,
		spawnRadius: Double
	): Double {
		if (direction.axis == axis) {
			return random.nextDouble() * spawnRadius * direction.axisDirection.step
		}

		return (random.nextDouble() * 2 - 1) * spawnRadius
	}

	override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
		super.saveAdditional(tag, registries)
		pollenSpotHandler.savePersistentData(tag)
	}

	override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
		super.loadAdditional(tag, registries)
		pollenSpotHandler.loadPersistentData(tag)
	}

	override fun getUpdateTag(pRegistries: HolderLookup.Provider): CompoundTag {
		val tag = saveWithoutMetadata(pRegistries)
		pollenSpotHandler.removePersistentData(tag)
		pollenSpotHandler.saveDisplayData(tag)
		return tag
	}

	override fun handleUpdateTag(tag: CompoundTag, lookupProvider: HolderLookup.Provider) {
		super.handleUpdateTag(tag, lookupProvider)
		pollenSpotHandler.loadDisplayData(tag)
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