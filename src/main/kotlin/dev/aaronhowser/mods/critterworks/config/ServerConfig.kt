package dev.aaronhowser.mods.critterworks.config

import net.neoforged.neoforge.common.ModConfigSpec
import org.apache.commons.lang3.tuple.Pair

class ServerConfig(
	private val builder: ModConfigSpec.Builder
) {

	lateinit var dyeberryVineReplacementChance: ModConfigSpec.DoubleValue
	lateinit var lockboxDropIntervalTicks: ModConfigSpec.IntValue
	lateinit var lockboxDropAmount: ModConfigSpec.IntValue
	lateinit var scoochwormAppleRarity: ModConfigSpec.IntValue
	lateinit var hoppingSpiderNestRarity: ModConfigSpec.IntValue

	init {
		general()
	}

	private fun general() {
		lockboxDropIntervalTicks = builder
			.comment("How often an upside-down Lockbox drops an item, in ticks.")
			.defineInRange("lockboxDropIntervalTicks", 2, 1, Int.MAX_VALUE)

		lockboxDropAmount = builder
			.comment("The number of items an upside-down Lockbox attempts to drop each interval.")
			.defineInRange("lockboxDropAmount", 1, 1, Int.MAX_VALUE)

		dyeberryVineReplacementChance = builder
			.comment("The chance that a berry-bearing cave vine is replaced with a dyeberry vine.")
			.defineInRange("dyeberryVineReplacementChance", 0.05, 0.0, 1.0)

		scoochwormAppleRarity = builder
			.comment("The average number of chunks between Scoochworm Apple generation attempts.")
			.defineInRange("scoochwormAppleRarity", 24, 1, Int.MAX_VALUE)

		hoppingSpiderNestRarity = builder
			.comment("The average number of chunks between Hopping Spider Nest generation attempts.")
			.defineInRange("hoppingSpiderNestRarity", 128, 1, Int.MAX_VALUE)
	}

	companion object {
		private val configPair: Pair<ServerConfig, ModConfigSpec> = ModConfigSpec.Builder().configure(::ServerConfig)

		val CONFIG: ServerConfig = configPair.left
		val CONFIG_SPEC: ModConfigSpec = configPair.right
	}
}