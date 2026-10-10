package dev.aaronhowser.mods.critterworks.block_entity

import dev.aaronhowser.mods.aaron.block_entity.SyncingBlockEntity
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.toBlockPos
import dev.aaronhowser.mods.critterworks.config.ServerConfig
import dev.aaronhowser.mods.critterworks.registry.ModBlockEntityTypes
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.energy.IEnergyStorage

class SparkbugBushBlockEntity(
	pos: BlockPos,
	blockState: BlockState
) : SyncingBlockEntity(ModBlockEntityTypes.SPARKBUG_BUSH.get(), pos, blockState) {

	override val syncImmediately: Boolean = true
	private val energyStorage = DistributedEnergyStorage()

	val inputPollenSpots: Set<PollenSpot>
		field = mutableSetOf()

	val outputPollenSpots: Set<PollenSpot>
		field = mutableSetOf()

	fun addPollenSpot(
		pos: BlockPos,
		direction: Direction,
		isInput: Boolean
	): Boolean {
		val level = level ?: return false

		val spot = PollenSpot(pos, direction)
		if (spot in inputPollenSpots || spot in outputPollenSpots) return false
		if (spot.getEnergyHandler(level) == null) return false

		val success = if (isInput) {
			inputPollenSpots.add(spot)
		} else {
			outputPollenSpots.add(spot)
		}

		if (!success) return false

		setChanged()
		return true
	}

	fun removePollenSpot(
		pos: BlockPos,
		direction: Direction
	): Boolean {
		val success = inputPollenSpots.removeIf { it.pos == pos && it.direction == direction }
			|| outputPollenSpots.removeIf { it.pos == pos && it.direction == direction }

		if (success) setChanged()
		return success
	}

	private fun serverTick(level: ServerLevel) {
		val maximumTransfer = ServerConfig.CONFIG.sparkbugBushMaxEnergyTransferPerInput.get()

		for (inputHandler in getInputHandlers(level)) {
			if (!inputHandler.canExtract()) continue

			val availableEnergy = inputHandler.extractEnergy(maximumTransfer, true)
			if (availableEnergy <= 0) continue

			val acceptedEnergy = energyStorage.receiveEnergy(availableEnergy, true)
			if (acceptedEnergy <= 0) continue

			val extractedEnergy = inputHandler.extractEnergy(acceptedEnergy, false)
			if (extractedEnergy <= 0) continue

			energyStorage.receiveEnergy(extractedEnergy, false)
		}
	}

	private fun getInputHandlers(level: Level?): List<IEnergyStorage> {
		if (level == null) return emptyList()
		return inputPollenSpots.mapNotNull { it.getEnergyHandler(level) }
	}

	private fun getOutputHandlers(level: Level?): List<IEnergyStorage> {
		if (level == null) return emptyList()
		return outputPollenSpots.mapNotNull { it.getEnergyHandler(level) }
	}

	override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
		super.saveAdditional(tag, registries)

		val inputSpotsTag = ListTag()
		for (spot in inputPollenSpots) {
			inputSpotsTag.add(spot.toTag())
		}

		val outputSpotsTag = ListTag()
		for (spot in outputPollenSpots) {
			outputSpotsTag.add(spot.toTag())
		}

		tag.put(INPUT_SPOTS_TAG, inputSpotsTag)
		tag.put(OUTPUT_SPOTS_TAG, outputSpotsTag)
	}

	override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
		super.loadAdditional(tag, registries)

		inputPollenSpots.clear()
		outputPollenSpots.clear()

		val inputSpotsTag = tag.getList(INPUT_SPOTS_TAG, Tag.TAG_COMPOUND.toInt())
		for (i in inputSpotsTag.indices) {
			val spotTag = inputSpotsTag.getCompound(i)
			inputPollenSpots.add(PollenSpot.fromTag(spotTag))
		}

		val outputSpotsTag = tag.getList(OUTPUT_SPOTS_TAG, Tag.TAG_COMPOUND.toInt())
		for (i in outputSpotsTag.indices) {
			val spotTag = outputSpotsTag.getCompound(i)
			outputPollenSpots.add(PollenSpot.fromTag(spotTag))
		}
	}

	companion object {
		const val INPUT_SPOTS_TAG = "input_spots"
		const val OUTPUT_SPOTS_TAG = "output_spots"

		fun tick(
			level: Level,
			blockPos: BlockPos,
			blockState: BlockState,
			blockEntity: SparkbugBushBlockEntity
		) {
			if (level is ServerLevel) {
				blockEntity.serverTick(level)
			}
		}
	}

	inner class DistributedEnergyStorage : IEnergyStorage {
		private var nextOutputIndex = 0

		override fun receiveEnergy(toReceive: Int, simulate: Boolean): Int {
			if (toReceive <= 0) return 0

			val outputHandlers = mutableListOf<IEnergyStorage>()
			for (outputHandler in getOutputHandlers(level)) {
				if (outputHandler.canReceive()) {
					outputHandlers.add(outputHandler)
				}
			}

			if (outputHandlers.isEmpty()) return 0
			if (simulate) return getSimulatedReceivedEnergy(outputHandlers, toReceive)

			val startingIndex = nextOutputIndex % outputHandlers.size
			var amountReceived = 0

			while (amountReceived < toReceive) {
				var receivedThisPass = 0

				for (offset in outputHandlers.indices) {
					val outputIndex = (startingIndex + offset) % outputHandlers.size
					val received = outputHandlers[outputIndex].receiveEnergy(1, false)
					if (received <= 0) continue

					amountReceived += received
					receivedThisPass += received
					nextOutputIndex = (outputIndex + 1) % outputHandlers.size

					if (amountReceived >= toReceive) break
				}

				if (receivedThisPass == 0) break
			}

			return amountReceived
		}

		private fun getSimulatedReceivedEnergy(
			outputHandlers: List<IEnergyStorage>,
			toReceive: Int
		): Int {
			var amountReceived = 0L

			for (outputHandler in outputHandlers) {
				amountReceived += outputHandler.receiveEnergy(toReceive, true).toLong()
				if (amountReceived >= toReceive) return toReceive
			}

			return amountReceived.toInt()
		}

		override fun extractEnergy(toExtract: Int, simulate: Boolean): Int {
			return 0
		}

		override fun getEnergyStored(): Int = getOutputHandlers(level).sumOf { it.energyStored.toLong() }.toInt()
		override fun getMaxEnergyStored(): Int = getOutputHandlers(level).sumOf { it.maxEnergyStored.toLong() }.toInt()
		override fun canExtract(): Boolean = false
		override fun canReceive(): Boolean = getOutputHandlers(level).any(IEnergyStorage::canReceive)
	}

	data class PollenSpot(
		val pos: BlockPos,
		val direction: Direction
	) {

		fun getEnergyHandler(level: Level): IEnergyStorage? {
			return level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, direction)
		}

		fun toTag(): CompoundTag {
			val tag = CompoundTag()
			tag.putLong(POS_TAG, pos.asLong())
			tag.putInt(DIRECTION_TAG, direction.ordinal)
			return tag
		}

		companion object {
			const val POS_TAG = "pos"
			const val DIRECTION_TAG = "direction"

			fun fromTag(tag: CompoundTag): PollenSpot {
				val pos = tag.getLong(POS_TAG).toBlockPos()
				val directionOrdinal = tag.getInt(DIRECTION_TAG)
				val direction = Direction.entries[directionOrdinal]
				return PollenSpot(pos, direction)
			}
		}
	}

}