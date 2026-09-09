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

		addConfig("scoochworm_attachments", "Scoochworm Attachments")
		addConfig(ServerConfig.CONFIG.lockboxDropIntervalTicks, "Lockbox Drop Interval (ticks)")
		addConfig(ServerConfig.CONFIG.lockboxDropAmount, "Lockbox Drop Amount")
		addConfig(ServerConfig.CONFIG.maxChunkLoadersPerPlayer, "Maximum Chunk Loaders per Player")

		addConfig("world_gen", "World Generation")
		addConfig(ServerConfig.CONFIG.dyeberryVineReplacementChance, "Dyeberry Vine Replacement Chance")
		addConfig(ServerConfig.CONFIG.scoochwormAppleRarity, "Scoochworm Apple Rarity")
		addConfig(ServerConfig.CONFIG.hoppingSpiderNestRarity, "Hopping Spider Nest Rarity")

		addConfig("debug", "Debug")
		addConfig(ServerConfig.CONFIG.sendScoochwormAppleTeleportMessage, "Send Scoochworm Apple Teleport Message")
		addConfig(ServerConfig.CONFIG.sendHoppingSpiderNestTeleportMessage, "Send Hopping Spider Nest Teleport Message")
		addConfig(ClientConfig.CONFIG.renderScoochwormAttachmentProbe, "Render Scoochworm Attachment Probe")
		addConfig(ClientConfig.CONFIG.renderScoochwormPath, "Render Scoochworm Path")
		addConfig(ClientConfig.CONFIG.renderWebLineDebugColors, "Render Web Line Debug Colors")
	}
}