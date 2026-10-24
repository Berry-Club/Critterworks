package dev.aaronhowser.mods.critterworks.datagen.language

import dev.aaronhowser.mods.critterworks.config.ClientConfig
import dev.aaronhowser.mods.critterworks.config.ServerConfig
import net.neoforged.neoforge.common.ModConfigSpec

object ModConfigLang {

	fun add(provider: ModLanguageProvider) {
		fun addConfig(name: String, value: String) {
			provider.add("critterworks.configuration.$name", value)
		}

		fun addConfig(config: ModConfigSpec.ConfigValue<*>, value: String) {
			addConfig(config.path.last(), value)
		}

		addConfig(ServerConfig.CONFIG.tardigradeDisablesExpiring, "Tardigrades Prevent Item Expiration")
		addConfig(ServerConfig.CONFIG.filterfeederWaterFogRadius, "Filterfeeder Water Fog Radius")

		addConfig("scoochworm_attachments", "Scoochworm Attachments")
		addConfig(ServerConfig.CONFIG.lockboxDropIntervalTicks, "Lockbox Drop Interval (ticks)")
		addConfig(ServerConfig.CONFIG.lockboxDropAmount, "Lockbox Drop Amount")
		addConfig(ServerConfig.CONFIG.maxChunkLoadersPerPlayer, "Maximum Chunk Loaders per Player")

		addConfig("world_gen", "World Generation")
		addConfig(ServerConfig.CONFIG.dyeberryVineReplacementChance, "Dyeberry Vine Replacement Chance")
		addConfig(ServerConfig.CONFIG.scoochwormAppleRarity, "Scoochworm Apple Rarity")
		addConfig(ServerConfig.CONFIG.hoppingSpiderNestRarity, "Hopping Spider Nest Rarity")

		addConfig("sparkbug_bush", "Sparkbug Bush")
		addConfig(ServerConfig.CONFIG.sparkbugBushMaxEnergyTransferPerInput, "Maximum Energy Transfer per Input (FE/t)")
		addConfig(ServerConfig.CONFIG.sparkbugBushPollenRadius, "Pollen Point Radius")

		addConfig("punchprawn", "Punchprawn")
		addConfig(ServerConfig.CONFIG.wornPunchprawnCooldown, "Worn Punchprawn Cooldown")
		addConfig(ServerConfig.CONFIG.wornPunchprawnKnockback, "Worn Punchprawn Knockback")
		addConfig(ServerConfig.CONFIG.wornPunchprawnDamageBoost, "Worn Punchprawn Damage Boost")

		addConfig("sproutsnail", "Sproutsnail")
		addConfig(ServerConfig.CONFIG.sproutsnailBonemealChance, "Sproutsnail Bonemeal Chance")
		addConfig(ServerConfig.CONFIG.sproutsnailEvilChance, "Evil Sproutsnail Chance")
		addConfig(ServerConfig.CONFIG.sproutsnailEvilSpeed, "Evil Sproutsnail Speed")

		addConfig("rendering", "Rendering")
		addConfig(ClientConfig.CONFIG.hoppingSpiderScale, "Hopping Spider Scale")
		addConfig(ClientConfig.CONFIG.rotateScoochwormPassengerCamera, "Rotate Scoochworm Passenger Camera")
		addConfig(ClientConfig.CONFIG.fireflyParticleSpawnChance, "Firefly Particle Spawn Chance")
		addConfig(ClientConfig.CONFIG.fireflyParticleSpawnRadius, "Firefly Particle Spawn Radius")

		addConfig("debug", "Debug")
		addConfig(ServerConfig.CONFIG.sendScoochwormAppleTeleportMessage, "Send Scoochworm Apple Teleport Message")
		addConfig(ServerConfig.CONFIG.sendHoppingSpiderNestTeleportMessage, "Send Hopping Spider Nest Teleport Message")
		addConfig(ServerConfig.CONFIG.sendScoochstemVineTeleportMessage, "Send Scoochstem Vine Teleport Message")
		addConfig(ClientConfig.CONFIG.renderScoochwormAttachmentProbe, "Render Scoochworm Attachment Probe")
		addConfig(ClientConfig.CONFIG.renderScoochwormPath, "Render Scoochworm Path")
		addConfig(ClientConfig.CONFIG.renderWebLineDebugColors, "Render Web Line Debug Colors")
	}
}