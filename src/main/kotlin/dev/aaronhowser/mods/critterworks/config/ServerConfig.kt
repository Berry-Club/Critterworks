package dev.aaronhowser.mods.critterworks.config

import dev.aaronhowser.mods.aaron.misc.AaronDsls.section
import net.neoforged.neoforge.common.ModConfigSpec
import org.apache.commons.lang3.tuple.Pair

class ServerConfig(
	private val builder: ModConfigSpec.Builder
) {

	lateinit var lockboxDropIntervalTicks: ModConfigSpec.IntValue
	lateinit var lockboxDropAmount: ModConfigSpec.IntValue
	lateinit var maxChunkLoadersPerPlayer: ModConfigSpec.IntValue

	lateinit var dyeberryVineReplacementChance: ModConfigSpec.DoubleValue
	lateinit var scoochwormAppleRarity: ModConfigSpec.IntValue
	lateinit var hoppingSpiderNestRarity: ModConfigSpec.IntValue

	lateinit var sendScoochwormAppleTeleportMessage: ModConfigSpec.BooleanValue
	lateinit var sendHoppingSpiderNestTeleportMessage: ModConfigSpec.BooleanValue

	init {
		general()
	}

	private fun general() {
		builder.section("scoochworm_attachments") {
			scoochwormAttachmentConfigs()
		}

		builder.section("world_gen") {
			worldGenConfigs()
		}

		builder.section("debug") {
			debugConfigs()
		}
	}

	private fun scoochwormAttachmentConfigs() {
		lockboxDropIntervalTicks = builder
			.comment("How often an upside-down Lockbox drops an item, in ticks.")
			.defineInRange("lockboxDropIntervalTicks", 2, 1, Int.MAX_VALUE)

		lockboxDropAmount = builder
			.comment("The number of items an upside-down Lockbox attempts to drop each interval.")
			.defineInRange("lockboxDropAmount", 1, 1, Int.MAX_VALUE)

		maxChunkLoadersPerPlayer = builder
			.comment("How many Chunkloader Attachments can each player own?")
			.defineInRange("maxChunkLoadersPerPlayer", 15, 1, Int.MAX_VALUE)
	}

	private fun worldGenConfigs() {
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

	private fun debugConfigs() {
		sendScoochwormAppleTeleportMessage = builder
			.comment("Send a clickable teleport message when a Scoochworm Apple generates.")
			.define("sendScoochwormAppleTeleportMessage", false)

		sendHoppingSpiderNestTeleportMessage = builder
			.comment("Send a clickable teleport message when a Hopping Spider Nest generates.")
			.define("sendHoppingSpiderNestTeleportMessage", false)
	}

	companion object {
		private val configPair: Pair<ServerConfig, ModConfigSpec> = ModConfigSpec.Builder().configure(::ServerConfig)

		val CONFIG: ServerConfig = configPair.left
		val CONFIG_SPEC: ModConfigSpec = configPair.right
	}
}