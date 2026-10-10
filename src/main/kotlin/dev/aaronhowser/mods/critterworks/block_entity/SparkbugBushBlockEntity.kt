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

	private val inputPollenSpots: MutableSet<PollenSpot> = mutableSetOf()
	private val outputPollenSpots: MutableSet<PollenSpot> = mutableSetOf()

	var displayPollenSpots: Set<DisplayPollenSpot> = setOf()
		private set

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

		val displaySpots = mutableSetOf<DisplayPollenSpot>()
		energyStorage.resetTickActivity()

		for (inputSpot in inputPollenSpots) {
			val inputHandler = inputSpot.getEnergyHandler(level) ?: continue
			val isActive = transferEnergyFromInput(inputHandler, maximumTransfer)
			displaySpots += inputSpot.toDisplaySpot(isInput = true, isActive)
		}

		for (outputSpot in outputPollenSpots) {
			if (outputSpot.getEnergyHandler(level) == null) continue

			val isActive = energyStorage.wasActiveThisTick(outputSpot)
			displaySpots += outputSpot.toDisplaySpot(isInput = false, isActive)
		}

		if (displaySpots != displayPollenSpots) {
			displayPollenSpots = displaySpots
			setChanged()
		}
	}

	private fun transferEnergyFromInput(
		inputHandler: IEnergyStorage,
		maximumTransfer: Int
	): Boolean {
		if (!inputHandler.canExtract()) return false

		val availableEnergy = inputHandler.extractEnergy(maximumTransfer, true)
		if (availableEnergy <= 0) return false

		val acceptedEnergy = energyStorage.receiveEnergy(availableEnergy, true)
		if (acceptedEnergy <= 0) return false

		val extractedEnergy = inputHandler.extractEnergy(acceptedEnergy, false)
		if (extractedEnergy <= 0) return false

		energyStorage.receiveEnergy(extractedEnergy, false)
		return true
	}

	private fun getOutputHandlers(level: Level?): List<IEnergyStorage> {
		if (level == null) return emptyList()
		return outputPollenSpots.mapNotNull { it.getEnergyHandler(level) }
	}

	override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
		super.saveAdditional(tag, registries)

		val inputSpotsTag = ListTag()
		for (spot in inputPollenSpots) {
			inputSpotsTag += spot.toTag()
		}

		val outputSpotsTag = ListTag()
		for (spot in outputPollenSpots) {
			outputSpotsTag += spot.toTag()
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
			inputPollenSpots += PollenSpot.fromTag(spotTag)
		}

		val outputSpotsTag = tag.getList(OUTPUT_SPOTS_TAG, Tag.TAG_COMPOUND.toInt())
		for (i in outputSpotsTag.indices) {
			val spotTag = outputSpotsTag.getCompound(i)
			outputPollenSpots += PollenSpot.fromTag(spotTag)
		}
	}

	override fun getUpdateTag(pRegistries: HolderLookup.Provider): CompoundTag {
		val tag = saveWithoutMetadata(pRegistries)
		tag.remove(INPUT_SPOTS_TAG)
		tag.remove(OUTPUT_SPOTS_TAG)

		val displaySpotsTag = ListTag()
		for (spot in displayPollenSpots) {
			displaySpotsTag += spot.toTag()
		}

		tag.put(DISPLAY_SPOTS_TAG, displaySpotsTag)
		return tag
	}

	override fun handleUpdateTag(tag: CompoundTag, lookupProvider: HolderLookup.Provider) {
		super.handleUpdateTag(tag, lookupProvider)

		val displaySpots = mutableSetOf<DisplayPollenSpot>()

		val visibleSpotsTag = tag.getList(DISPLAY_SPOTS_TAG, Tag.TAG_COMPOUND.toInt())
		for (i in visibleSpotsTag.indices) {
			val spotTag = visibleSpotsTag.getCompound(i)
			displaySpots += DisplayPollenSpot.fromTag(spotTag)
		}

		displayPollenSpots = displaySpots
	}

	companion object {
		const val INPUT_SPOTS_TAG = "input_spots"
		const val OUTPUT_SPOTS_TAG = "output_spots"
		const val DISPLAY_SPOTS_TAG = "display_spots"

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
		private val activeOutputSpots: MutableSet<PollenSpot> = mutableSetOf()

		fun resetTickActivity() {
			activeOutputSpots.clear()
		}

		fun wasActiveThisTick(pollenSpot: PollenSpot): Boolean {
			return pollenSpot in activeOutputSpots
		}

		override fun receiveEnergy(toReceive: Int, simulate: Boolean): Int {
			if (toReceive <= 0) return 0
			val currentLevel = level ?: return 0

			val outputHandlers = mutableListOf<ResolvedPollenSpot>()
			for (outputSpot in outputPollenSpots) {
				val outputHandler = outputSpot.getEnergyHandler(currentLevel) ?: continue
				if (outputHandler.canReceive()) {
					outputHandlers += ResolvedPollenSpot(outputSpot, outputHandler)
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
					val output = outputHandlers[outputIndex]
					val received = output.energyHandler.receiveEnergy(1, false)
					if (received <= 0) continue

					amountReceived += received
					receivedThisPass += received
					activeOutputSpots += output.pollenSpot
					nextOutputIndex = (outputIndex + 1) % outputHandlers.size

					if (amountReceived >= toReceive) break
				}

				if (receivedThisPass == 0) break
			}

			return amountReceived
		}

		private fun getSimulatedReceivedEnergy(
			outputHandlers: List<ResolvedPollenSpot>,
			toReceive: Int
		): Int {
			var amountReceived = 0L

			for (output in outputHandlers) {
				amountReceived += output.energyHandler.receiveEnergy(toReceive, true).toLong()
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
		fun toDisplaySpot(isInput: Boolean, isActive: Boolean): DisplayPollenSpot {
			return DisplayPollenSpot(pos, direction, isInput, isActive)
		}

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

	private data class ResolvedPollenSpot(
		val pollenSpot: PollenSpot,
		val energyHandler: IEnergyStorage
	)

	data class DisplayPollenSpot(
		val pos: BlockPos,
		val direction: Direction,
		val isInput: Boolean,
		val isActive: Boolean
	) {

		fun toTag(): CompoundTag {
			val tag = CompoundTag()
			tag.putLong(POS_TAG, pos.asLong())
			tag.putInt(DIRECTION_TAG, direction.ordinal)
			tag.putBoolean(IS_INPUT_TAG, isInput)
			tag.putBoolean(IS_ACTIVE_TAG, isActive)
			return tag
		}

		companion object {
			const val POS_TAG = "pos"
			const val DIRECTION_TAG = "direction"
			const val IS_INPUT_TAG = "is_input"
			const val IS_ACTIVE_TAG = "is_active"

			fun fromTag(tag: CompoundTag): DisplayPollenSpot {
				val pos = tag.getLong(POS_TAG).toBlockPos()
				val directionOrdinal = tag.getInt(DIRECTION_TAG)
				val direction = Direction.entries[directionOrdinal]
				val isInput = tag.getBoolean(IS_INPUT_TAG)
				val isActive = tag.getBoolean(IS_ACTIVE_TAG)
				return DisplayPollenSpot(pos, direction, isInput, isActive)
			}
		}
	}

}