package dev.aaronhowser.mods.critterworks.config

import dev.aaronhowser.mods.aaron.misc.AaronDsls.section
import net.neoforged.neoforge.common.ModConfigSpec
import org.apache.commons.lang3.tuple.Pair

class ClientConfig(
	private val builder: ModConfigSpec.Builder
) {

	lateinit var hoppingSpiderScale: ModConfigSpec.DoubleValue

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

		val CONFIG: ClientConfig = configPair.left
		val CONFIG_SPEC: ModConfigSpec = configPair.right
	}
}