package dev.aaronhowser.mods.critterworks.datagen.language

object ModMessageLang {

	const val FIRST_NODE_MESSAGE = "item.critterworks.artificial_spinnerets.first_node"
	const val SAME_LINE_MESSAGE = "item.critterworks.artificial_spinnerets.same_line"
	const val SAME_DIRECTION_MESSAGE = "item.critterworks.artificial_spinnerets.same_direction"
	const val TOO_LONG_MESSAGE = "item.critterworks.artificial_spinnerets.too_long"
	const val NOT_ENOUGH_WEB_FLUID_MESSAGE = "item.critterworks.artificial_spinnerets.not_enough_web_fluid"
	const val OBSTRUCTED_MESSAGE = "item.critterworks.artificial_spinnerets.obstructed"
	const val LINE_CREATED_MESSAGE = "item.critterworks.artificial_spinnerets.line_created"
	const val LINE_REMOVED_MESSAGE = "item.critterworks.artificial_spinnerets.line_removed"
	const val POLLEN_POINT_TOO_FAR_MESSAGE = "item.critterworks.sparkpollen_pouch.too_far"
	const val POLLEN_POINT_NO_ENERGY_CONNECTION_MESSAGE = "item.critterworks.sparkpollen_pouch.no_energy_connection"
	const val EVIL_SNAIL_CAGE_MESSAGE = "message.critterworks.evil_snail_cage"
	const val EVIL_SNAIL_SHIELD_MESSAGE = "message.critterworks.evil_snail_shield"
	const val EVIL_SNAIL_BANISHED_MESSAGE = "message.critterworks.evil_snail_banished"

	fun add(provider: ModLanguageProvider) {
		provider.apply {
			add(FIRST_NODE_MESSAGE, "First web endpoint selected")
			add(SAME_LINE_MESSAGE, "A web line cannot attach to the same line twice")
			add(SAME_DIRECTION_MESSAGE, "The selected faces cannot point in the same direction")
			add(TOO_LONG_MESSAGE, "The web line must be shorter than 10 blocks")
			add(NOT_ENOUGH_WEB_FLUID_MESSAGE, "The Spinnerets do not have enough web fluid")
			add(OBSTRUCTED_MESSAGE, "The selected faces do not have line of sight")
			add(LINE_CREATED_MESSAGE, "Web line created")
			add(LINE_REMOVED_MESSAGE, "Web line removed")
			add(POLLEN_POINT_TOO_FAR_MESSAGE, "The Pollen Point is too far from the Sparkbug Bush")
			add(POLLEN_POINT_NO_ENERGY_CONNECTION_MESSAGE, "The selected face cannot transfer energy")
			add(EVIL_SNAIL_CAGE_MESSAGE, "You thought you caught it, but it was only a decoy snail...")
			add(EVIL_SNAIL_SHIELD_MESSAGE, "The snail was temporarily banished, but not forever...")
			add(EVIL_SNAIL_BANISHED_MESSAGE, "The snail struck your shield one last time and banished for good.")
		}
	}

}