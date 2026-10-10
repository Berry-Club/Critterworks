package dev.aaronhowser.mods.critterworks.handler.sparkbug

import net.minecraft.world.level.Level
import net.neoforged.neoforge.energy.IEnergyStorage

class PollenEnergyDistributor {

	private var nextOutputIndex = 0
	private val activeOutputSpots: MutableSet<PollenSpot> = mutableSetOf()

	fun resetTickActivity() {
		activeOutputSpots.clear()
	}

	fun wasActiveThisTick(pollenSpot: PollenSpot): Boolean {
		return pollenSpot in activeOutputSpots
	}

	fun simulateReceive(
		level: Level,
		outputPollenSpots: Collection<PollenSpot>,
		amount: Int
	): Int {
		val resolvedOutputSpots = getResolvedOutputSpots(level, outputPollenSpots)
		var amountReceived = 0L

		for (output in resolvedOutputSpots) {
			amountReceived += output.energyHandler.receiveEnergy(amount, true).toLong()
			if (amountReceived >= amount) return amount
		}

		return amountReceived.toInt()
	}

	fun receive(
		level: Level,
		outputPollenSpots: Collection<PollenSpot>,
		amount: Int
	): Int {
		if (amount <= 0) return 0

		val resolvedOutputSpots = getResolvedOutputSpots(level, outputPollenSpots)
		if (resolvedOutputSpots.isEmpty()) return 0

		val startingIndex = nextOutputIndex % resolvedOutputSpots.size
		var amountReceived = 0

		while (amountReceived < amount) {
			var receivedThisPass = 0

			for (offset in resolvedOutputSpots.indices) {
				val outputIndex = (startingIndex + offset) % resolvedOutputSpots.size
				val output = resolvedOutputSpots[outputIndex]
				val received = output.energyHandler.receiveEnergy(1, false)
				if (received <= 0) continue

				amountReceived += received
				receivedThisPass += received
				activeOutputSpots += output.pollenSpot
				nextOutputIndex = (outputIndex + 1) % resolvedOutputSpots.size

				if (amountReceived >= amount) break
			}

			if (receivedThisPass == 0) break
		}

		return amountReceived
	}

	private fun getResolvedOutputSpots(
		level: Level,
		outputPollenSpots: Collection<PollenSpot>
	): List<ResolvedPollenSpot> {
		val resolvedOutputSpots = mutableListOf<ResolvedPollenSpot>()

		for (outputSpot in outputPollenSpots) {
			val outputHandler = outputSpot.getEnergyHandler(level) ?: continue
			if (!outputHandler.canReceive()) continue

			resolvedOutputSpots += ResolvedPollenSpot(outputSpot, outputHandler)
		}

		return resolvedOutputSpots
	}

	private data class ResolvedPollenSpot(
		val pollenSpot: PollenSpot,
		val energyHandler: IEnergyStorage
	)

}