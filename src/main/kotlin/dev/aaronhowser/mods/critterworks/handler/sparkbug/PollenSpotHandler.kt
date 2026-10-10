package dev.aaronhowser.mods.critterworks.handler.sparkbug

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import net.neoforged.neoforge.energy.IEnergyStorage

class PollenSpotHandler {

	private val inputPollenSpots: MutableSet<PollenSpot> = mutableSetOf()
	private val outputPollenSpots: MutableSet<PollenSpot> = mutableSetOf()
	private val energyDistributor = PollenEnergyDistributor()

	private var serverDisplayPollenSpots: Set<DisplayPollenSpot> = setOf()
	private val remainingActiveDisplayTicks: MutableMap<PollenSpot, Int> = mutableMapOf()

	var displayPollenSpots: Set<DisplayPollenSpot> = setOf()
		private set

	fun addPollenSpot(
		level: Level,
		pos: BlockPos,
		direction: Direction,
		isInput: Boolean
	): Boolean {
		val pollenSpot = PollenSpot(pos, direction)
		if (pollenSpot in inputPollenSpots || pollenSpot in outputPollenSpots) return false
		if (pollenSpot.getEnergyHandler(level) == null) return false

		return if (isInput) {
			inputPollenSpots.add(pollenSpot)
		} else {
			outputPollenSpots.add(pollenSpot)
		}
	}

	fun removePollenSpot(pos: BlockPos, direction: Direction): Boolean {
		val pollenSpot = PollenSpot(pos, direction)
		return inputPollenSpots.remove(pollenSpot) || outputPollenSpots.remove(pollenSpot)
	}

	fun serverTick(level: ServerLevel, maximumTransfer: Int): Boolean {
		val displaySpots = mutableSetOf<DisplayPollenSpot>()
		energyDistributor.resetTickActivity()

		for (inputSpot in inputPollenSpots) {
			val inputHandler = inputSpot.getEnergyHandler(level) ?: continue
			val isActive = transferEnergyFromInput(level, inputHandler, maximumTransfer)
			displaySpots += inputSpot.toDisplaySpot(isInput = true, isActive)
		}

		for (outputSpot in outputPollenSpots) {
			if (outputSpot.getEnergyHandler(level) == null) continue

			val isActive = energyDistributor.wasActiveThisTick(outputSpot)
			displaySpots += outputSpot.toDisplaySpot(isInput = false, isActive)
		}

		if (displaySpots == displayPollenSpots) return false

		displayPollenSpots = displaySpots
		return true
	}

	fun clientTick() {
		updateClientDisplaySpots(advanceTimers = true)
	}

	private fun transferEnergyFromInput(
		level: ServerLevel,
		inputHandler: IEnergyStorage,
		maximumTransfer: Int
	): Boolean {
		if (!inputHandler.canExtract()) return false

		val availableEnergy = inputHandler.extractEnergy(maximumTransfer, true)
		if (availableEnergy <= 0) return false

		val acceptedEnergy = energyDistributor.simulateReceive(level, outputPollenSpots, availableEnergy)
		if (acceptedEnergy <= 0) return false

		val extractedEnergy = inputHandler.extractEnergy(acceptedEnergy, false)
		if (extractedEnergy <= 0) return false

		energyDistributor.receive(level, outputPollenSpots, extractedEnergy)
		return true
	}

	private fun updateClientDisplaySpots(advanceTimers: Boolean) {
		val updatedActiveDisplayTicks = mutableMapOf<PollenSpot, Int>()
		val updatedDisplaySpots = mutableSetOf<DisplayPollenSpot>()

		for (displaySpot in serverDisplayPollenSpots) {
			val pollenSpot = displaySpot.toPollenSpot()
			val previousTicks = remainingActiveDisplayTicks[pollenSpot] ?: 0

			val remainingTicks = when {
				displaySpot.isActive -> ACTIVE_DISPLAY_DURATION_TICKS
				advanceTimers -> previousTicks - 1
				else -> previousTicks
			}

			if (remainingTicks > 0) {
				updatedActiveDisplayTicks[pollenSpot] = remainingTicks
			}

			updatedDisplaySpots += displaySpot.copy(isActive = remainingTicks > 0)
		}

		remainingActiveDisplayTicks.clear()
		remainingActiveDisplayTicks.putAll(updatedActiveDisplayTicks)
		displayPollenSpots = updatedDisplaySpots
	}

	fun savePersistentData(tag: CompoundTag) {
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

	fun loadPersistentData(tag: CompoundTag) {
		inputPollenSpots.clear()
		outputPollenSpots.clear()

		val inputSpotsTag = tag.getList(INPUT_SPOTS_TAG, Tag.TAG_COMPOUND.toInt())
		for (index in inputSpotsTag.indices) {
			val spotTag = inputSpotsTag.getCompound(index)
			inputPollenSpots += PollenSpot.fromTag(spotTag)
		}

		val outputSpotsTag = tag.getList(OUTPUT_SPOTS_TAG, Tag.TAG_COMPOUND.toInt())
		for (index in outputSpotsTag.indices) {
			val spotTag = outputSpotsTag.getCompound(index)
			outputPollenSpots += PollenSpot.fromTag(spotTag)
		}
	}

	fun removePersistentData(tag: CompoundTag) {
		tag.remove(INPUT_SPOTS_TAG)
		tag.remove(OUTPUT_SPOTS_TAG)
	}

	fun saveDisplayData(tag: CompoundTag) {
		val displaySpotsTag = ListTag()
		for (spot in displayPollenSpots) {
			displaySpotsTag += spot.toTag()
		}

		tag.put(DISPLAY_SPOTS_TAG, displaySpotsTag)
	}

	fun loadDisplayData(tag: CompoundTag) {
		val displaySpots = mutableSetOf<DisplayPollenSpot>()
		val displaySpotsTag = tag.getList(DISPLAY_SPOTS_TAG, Tag.TAG_COMPOUND.toInt())

		for (index in displaySpotsTag.indices) {
			val spotTag = displaySpotsTag.getCompound(index)
			displaySpots += DisplayPollenSpot.fromTag(spotTag)
		}

		serverDisplayPollenSpots = displaySpots
		updateClientDisplaySpots(advanceTimers = false)
	}

	companion object {
		private const val ACTIVE_DISPLAY_DURATION_TICKS = 20

		private const val INPUT_SPOTS_TAG = "input_spots"
		private const val OUTPUT_SPOTS_TAG = "output_spots"
		private const val DISPLAY_SPOTS_TAG = "display_spots"
	}

}