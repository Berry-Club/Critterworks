package dev.aaronhowser.mods.critterworks.handler.sparkbug

import dev.aaronhowser.mods.aaron.misc.AaronExtensions.toComponent
import dev.aaronhowser.mods.critterworks.config.ServerConfig
import dev.aaronhowser.mods.critterworks.datagen.language.ModMessageLang
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import net.neoforged.neoforge.energy.IEnergyStorage

class PollenPointHandler {

	private val inputPollenPoints = mutableSetOf<PollenPoint>()
	private val outputPollenPoints = mutableSetOf<PollenPoint>()
	private val energyDistributor = PollenEnergyDistributor()

	private var serverDisplayPollenPoints = setOf<DisplayPollenPoint>()
	private val remainingActiveDisplayTicks = mutableMapOf<PollenPoint, Int>()

	var displayPollenPoints = setOf<DisplayPollenPoint>()
		private set

	fun togglePollenPoint(
		level: Level,
		bushPos: BlockPos,
		pos: BlockPos,
		direction: Direction,
		isInput: Boolean
	): PollenPointInteractionResult {
		val pollenPoint = PollenPoint(pos, direction)
		val removedInput = inputPollenPoints.remove(pollenPoint)
		val removedOutput = outputPollenPoints.remove(pollenPoint)
		if (removedInput || removedOutput) return PollenPointInteractionResult.Removed

		val invalidMessage = getInvalidMessage(level, bushPos, pollenPoint)
		if (invalidMessage != null) {
			return PollenPointInteractionResult.Failed(invalidMessage)
		}

		if (isInput) {
			inputPollenPoints.add(pollenPoint)
		} else {
			outputPollenPoints.add(pollenPoint)
		}

		return PollenPointInteractionResult.Added
	}

	private fun getInvalidMessage(
		level: Level,
		bushPos: BlockPos,
		pollenPoint: PollenPoint
	): Component? {
		val maximumRadius = ServerConfig.CONFIG.sparkbugBushPollenRadius.get()
		if (bushPos.distSqr(pollenPoint.pos) > maximumRadius * maximumRadius) {
			return ModMessageLang.POLLEN_POINT_TOO_FAR_MESSAGE.toComponent()
		}

		if (pollenPoint.getEnergyHandler(level) == null) {
			return ModMessageLang.POLLEN_POINT_NO_ENERGY_CONNECTION_MESSAGE.toComponent()
		}

		return null
	}

	fun serverTick(level: ServerLevel): Boolean {
		val displaySpots = mutableSetOf<DisplayPollenPoint>()
		energyDistributor.resetTickActivity()

		val maximumTransfer = ServerConfig.CONFIG.sparkbugBushMaxEnergyTransferPerInput.get()

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