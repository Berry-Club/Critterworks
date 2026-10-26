package dev.aaronhowser.mods.critterworks.config

import dev.aaronhowser.mods.aaron.misc.AaronDsls.section
import net.neoforged.neoforge.common.ModConfigSpec
import org.apache.commons.lang3.tuple.Pair

class ServerConfig(
	private val builder: ModConfigSpec.Builder
) {

	lateinit var tardigradeDisablesExpiring: ModConfigSpec.BooleanValue
	lateinit var filterfeederWaterFogRadius: ModConfigSpec.DoubleValue

	lateinit var lockboxDropIntervalTicks: ModConfigSpec.IntValue
	lateinit var lockboxDropAmount: ModConfigSpec.IntValue
	lateinit var maxChunkLoadersPerPlayer: ModConfigSpec.IntValue

	lateinit var sparkbugBushMaxEnergyTransferPerInput: ModConfigSpec.IntValue
	lateinit var sparkbugBushPollenRadius: ModConfigSpec.DoubleValue

	lateinit var wornPunchprawnDamageBoost: ModConfigSpec.DoubleValue
	lateinit var wornPunchprawnKnockback: ModConfigSpec.DoubleValue
	lateinit var wornPunchprawnCooldown: ModConfigSpec.IntValue

	lateinit var sproutsnailBonemealChance: ModConfigSpec.DoubleValue
	lateinit var sproutsnailEvilChance: ModConfigSpec.DoubleValue
	lateinit var sproutsnailEvilSpeed: ModConfigSpec.DoubleValue
	lateinit var sproutsnailEvilCageRestartDistance: ModConfigSpec.DoubleValue

	lateinit var dyeberryVineReplacementChance: ModConfigSpec.DoubleValue
	lateinit var scoochwormAppleRarity: ModConfigSpec.IntValue
	lateinit var hoppingSpiderNestRarity: ModConfigSpec.IntValue

	lateinit var sendScoochwormAppleTeleportMessage: ModConfigSpec.BooleanValue
	lateinit var sendHoppingSpiderNestTeleportMessage: ModConfigSpec.BooleanValue
	lateinit var sendScoochstemVineTeleportMessage: ModConfigSpec.BooleanValue

	init {
		general()

		builder.section("scoochworm_attachments") {
			scoochwormAttachmentConfigs()
		}

		builder.section("sparkbug_bush") {
			sparkbugBushConfigs()
		}

		builder.section("punchprawn") {
			punchprawnConfigs()
		}

		builder.section("sproutsnail") {
			sproutsnailConfigs()
		}

		builder.section("world_gen") {
			worldGenConfigs()
		}

		builder.section("debug") {
			debugConfigs()
		}
	}

	private fun general() {
		tardigradeDisablesExpiring = builder
			.comment("Whether or not Tardigrades attached to items prevent the item entity from expiring")
			.define("tardigradeDisablesExpiring", true)

		filterfeederWaterFogRadius = builder
			.comment("The distance at which Filterfeeders begin clearing water fog. Set to 0 to disable")
			.defineInRange("waterFogRadius", 64.0, 0.0, Double.MAX_VALUE)
	}

	private fun sproutsnailConfigs() {
		sproutsnailBonemealChance = builder
			.comment("What chance the Sproutsnail has to successfully bonemeal the block it walks through")
			.defineInRange("sproutsnailBonemealChance", 0.2, 0.0, 1.0)

		sproutsnailEvilChance = builder
			.comment("What chance that a Sproutsnail killed by a player will come back for revenge")
			.defineInRange("sproutsnailEvilChance", 0.01, 0.0, 1.0)

		sproutsnailEvilSpeed = builder
			.comment("How fast the Immortal Snail moves towards the player it's hunting, in meters per tick")
			.defineInRange("sproutsnailEvilSpeed", 0.05, Double.MIN_VALUE, Double.MAX_VALUE)

		sproutsnailEvilCageRestartDistance = builder
			.comment("How far away the Immortal Snail restarts when caught with a Critter Cage")
			.defineInRange("sproutsnailEvilCageRestartDistance", 10_000.0, 0.0, Double.MAX_VALUE)
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

	private fun sparkbugBushConfigs() {
		sparkbugBushMaxEnergyTransferPerInput = builder
			.comment("The maximum energy a Sparkbug Bush pulls from each input per tick.")
			.defineInRange("sparkbugBushMaxEnergyTransferPerInput", 1024, 1, Int.MAX_VALUE)

		sparkbugBushPollenRadius = builder
			.comment("The maximum distance a Pollen Point can be from the Sparkbug Bush")
			.defineInRange("sparkbugBushPollenRadius", 16.0, 0.0, Double.MAX_VALUE)
	}

	private fun punchprawnConfigs() {
		wornPunchprawnDamageBoost = builder
			.comment("The additional attack damage of a bare-handed player wearing a Punchprawn")
			.defineInRange("punchprawnDamageBoost", 14.0, 0.0, Double.MAX_VALUE)

		wornPunchprawnKnockback = builder
			.comment("The additional attack knockback of a bare-handed player wearing a Punchprawn")
			.defineInRange("punchprawnKnockback", 6.0, 0.0, Double.MAX_VALUE)

		wornPunchprawnCooldown = builder
			.comment("How long the Punchprawn attack takes to fully charge, in ticks.")
			.defineInRange("punchprawnCooldown", 200, 0, Int.MAX_VALUE)
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

		sendScoochstemVineTeleportMessage = builder
			.comment("Send a clickable teleport message when a Scoochstem Vine generates.")
			.define("sendScoochstemVineTeleportMessage", false)
	}

	companion object {
		private val configPair: Pair<ServerConfig, ModConfigSpec> = ModConfigSpec.Builder().configure(::ServerConfig)

		val CONFIG: ServerConfig = configPair.left
		val CONFIG_SPEC: ModConfigSpec = configPair.right
	}
}