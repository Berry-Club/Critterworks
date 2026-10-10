package dev.aaronhowser.mods.critterworks.config

import dev.aaronhowser.mods.aaron.misc.AaronDsls.section
import net.neoforged.neoforge.common.ModConfigSpec
import org.apache.commons.lang3.tuple.Pair

class ClientConfig(
	private val builder: ModConfigSpec.Builder
) {

	lateinit var hoppingSpiderScale: ModConfigSpec.DoubleValue
	lateinit var rotateScoochwormPassengerCamera: ModConfigSpec.BooleanValue
	lateinit var fireflyParticleSpawnChance: ModConfigSpec.DoubleValue
	lateinit var fireflyParticleSpawnRadius: ModConfigSpec.DoubleValue

	lateinit var renderScoochwormAttachmentProbe: ModConfigSpec.BooleanValue
	lateinit var renderScoochwormPath: ModConfigSpec.BooleanValue
	lateinit var renderWebLineDebugColors: ModConfigSpec.BooleanValue

	init {
		general()
	}

	private fun general() {
		builder.section("rendering") {
			renderingConfigs()
		}

		builder.section("debug") {
			debugConfigs()
		}
	}

	private fun renderingConfigs() {
		hoppingSpiderScale = builder
			.comment("The scale used to render Hopping Spiders in their nest.")
			.defineInRange("hoppingSpiderScale", 0.5, 0.1, 2.0)

		rotateScoochwormPassengerCamera = builder
			.comment("Rotate the first-person camera to match the surface a ridden Scoochworm is walking on.")
			.define("rotateScoochwormPassengerCamera", true)

		fireflyParticleSpawnChance = builder
			.comment("The chance that each active pollen spot spawns a firefly particle each tick.")
			.defineInRange("fireflyParticleSpawnChance", 0.05, 0.0, 1.0)

		fireflyParticleSpawnRadius = builder
			.comment("The distance firefly particles can spawn from an active pollen spot.")
			.defineInRange("fireflyParticleSpawnRadius", 0.75, 0.0, 16.0)
	}

	private fun debugConfigs() {
		renderScoochwormAttachmentProbe = builder
			.comment("Render the Scoochworm attachment probe position through walls.")
			.define("renderScoochwormAttachmentProbe", false)

		renderScoochwormPath = builder
			.comment("Render the client-recorded path of each Scoochworm.")
			.define("renderScoochwormPath", false)

		renderWebLineDebugColors = builder
			.comment("Render each web line with a stable color derived from its UUID.")
			.define("renderWebLineDebugColors", false)
	}

	companion object {
		private val configPair: Pair<ClientConfig, ModConfigSpec> = ModConfigSpec.Builder().configure(::ClientConfig)

		@JvmField
		val CONFIG: ClientConfig = configPair.left
		val CONFIG_SPEC: ModConfigSpec = configPair.right

	}
}