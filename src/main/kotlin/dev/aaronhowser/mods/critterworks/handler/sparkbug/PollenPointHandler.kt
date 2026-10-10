package dev.aaronhowser.mods.critterworks.handler.sparkbug

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import net.neoforged.neoforge.energy.IEnergyStorage

class PollenPointHandler {

	private val inputPollenPoints: MutableSet<PollenPoint> = mutableSetOf()
	private val outputPollenPoints: MutableSet<PollenPoint> = mutableSetOf()
	private val energyDistributor = PollenEnergyDistributor()

	private var serverDisplayPollenPoints: Set<DisplayPollenPoint> = setOf()
	private val remainingActiveDisplayTicks: MutableMap<PollenPoint, Int> = mutableMapOf()

	var displayPollenPoints: Set<DisplayPollenPoint> = setOf()
		private set

	fun addPollenPoint(
		level: Level,
		pos: BlockPos,
		direction: Direction,
		isInput: Boolean
	): Boolean {
		val pollenPoint = PollenPoint(pos, direction)
		if (pollenPoint in inputPollenPoints || pollenPoint in outputPollenPoints) return false
		if (pollenPoint.getEnergyHandler(level) == null) return false

		return if (isInput) {
			inputPollenPoints.add(pollenPoint)
		} else {
			outputPollenPoints.add(pollenPoint)
		}
	}

	fun removePollenPoint(pos: BlockPos, direction: Direction): Boolean {
		val pollenPoint = PollenPoint(pos, direction)
		return inputPollenPoints.remove(pollenPoint) || outputPollenPoints.remove(pollenPoint)
	}

	fun serverTick(level: ServerLevel, maximumTransfer: Int): Boolean {
		val displaySpots = mutableSetOf<DisplayPollenPoint>()
		energyDistributor.resetTickActivity()

		for (inputSpot in inputPollenPoints) {
			val inputHandler = inputSpot.getEnergyHandler(level) ?: continue
			val isActive = transferEnergyFromInput(level, inputHandler, maximumTransfer)
			displaySpots += inputSpot.toDisplaySpot(isInput = true, isActive)
		}

		for (outputSpot in outputPollenPoints) {
			if (outputSpot.getEnergyHandler(level) == null) continue

			val isActive = energyDistributor.wasActiveThisTick(outputSpot)
			displaySpots += outputSpot.toDisplaySpot(isInput = false, isActive)
		}

		if (displaySpots == displayPollenPoints) return false

		displayPollenPoints = displaySpots
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

		val acceptedEnergy = energyDistributor.simulateReceive(level, outputPollenPoints, availableEnergy)
		if (acceptedEnergy <= 0) return false

		val extractedEnergy = inputHandler.extractEnergy(acceptedEnergy, false)
		if (extractedEnergy <= 0) return false

		energyDistributor.receive(level, outputPollenPoints, extractedEnergy)
		return true
	}

	private fun updateClientDisplaySpots(advanceTimers: Boolean) {
		val updatedActiveDisplayTicks = mutableMapOf<PollenPoint, Int>()
		val updatedDisplaySpots = mutableSetOf<DisplayPollenPoint>()

		for (displaySpot in serverDisplayPollenPoints) {
			val pollenSpot = displaySpot.toPollenPoint()
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
		displayPollenPoints = updatedDisplaySpots
	}

	fun savePersistentData(tag: CompoundTag) {
		val inputSpotsTag = ListTag()
		for (spot in inputPollenPoints) {
			inputSpotsTag += spot.toTag()
		}

		val outputSpotsTag = ListTag()
		for (spot in outputPollenPoints) {
			outputSpotsTag += spot.toTag()
		}

		tag.put(INPUT_SPOTS_TAG, inputSpotsTag)
		tag.put(OUTPUT_SPOTS_TAG, outputSpotsTag)
	}

	fun loadPersistentData(tag: CompoundTag) {
		inputPollenPoints.clear()
		outputPollenPoints.clear()

		val inputSpotsTag = tag.getList(INPUT_SPOTS_TAG, Tag.TAG_COMPOUND.toInt())
		for (index in inputSpotsTag.indices) {
			val spotTag = inputSpotsTag.getCompound(index)
			inputPollenPoints += PollenPoint.fromTag(spotTag)
		}

		val outputSpotsTag = tag.getList(OUTPUT_SPOTS_TAG, Tag.TAG_COMPOUND.toInt())
		for (index in outputSpotsTag.indices) {
			val spotTag = outputSpotsTag.getCompound(index)
			outputPollenPoints += PollenPoint.fromTag(spotTag)
		}
	}

	fun removePersistentData(tag: CompoundTag) {
		tag.remove(INPUT_SPOTS_TAG)
		tag.remove(OUTPUT_SPOTS_TAG)
	}

	fun saveDisplayData(tag: CompoundTag) {
		val displaySpotsTag = ListTag()
		for (spot in displayPollenPoints) {
			displaySpotsTag += spot.toTag()
		}

		tag.put(DISPLAY_SPOTS_TAG, displaySpotsTag)
	}

	fun loadDisplayData(tag: CompoundTag) {
		val displaySpots = mutableSetOf<DisplayPollenPoint>()
		val displaySpotsTag = tag.getList(DISPLAY_SPOTS_TAG, Tag.TAG_COMPOUND.toInt())

		for (index in displaySpotsTag.indices) {
			val spotTag = displaySpotsTag.getCompound(index)
			displaySpots += DisplayPollenPoint.fromTag(spotTag)
		}

		serverDisplayPollenPoints = displaySpots
		updateClientDisplaySpots(advanceTimers = false)
	}

	companion object {
		private const val ACTIVE_DISPLAY_DURATION_TICKS = 20

		private const val INPUT_SPOTS_TAG = "input_spots"
		private const val OUTPUT_SPOTS_TAG = "output_spots"
		private const val DISPLAY_SPOTS_TAG = "display_spots"
	}

}