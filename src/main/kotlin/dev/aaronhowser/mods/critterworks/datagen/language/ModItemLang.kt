package dev.aaronhowser.mods.critterworks.datagen.language

import dev.aaronhowser.mods.critterworks.registry.ModCreativeModeTabs
import dev.aaronhowser.mods.critterworks.registry.ModItems

object ModItemLang {

	fun add(provider: ModLanguageProvider) {
		provider.apply {
			add(ModCreativeModeTabs.CREATIVE_TAB_TRANSLATION_KEY, "Critterworks")
			addItem(ModItems.LOCKBOX, "Lockbox")
			addItem(ModItems.SCOOCHWORM_GPS, "Scoochworm GPS")
			addItem(ModItems.ARTIFICIAL_SPINNERETS, "Artificial Spinnerets")
			addItem(ModItems.WEBSTONE_WEAVER, "Webstone Weaver")
			addItem(ModItems.DIRECTIONAL_ARACHNODE, "Directional Arachnode")
			addItem(ModItems.INVERTING_ARACHNODE, "Inverting Arachnode")
			addItem(ModItems.ITEM_FILTERFEEDER, "Item Filterfeeder")
			addItem(ModItems.FLUID_FILTERFEEDER, "Fluid Filterfeeder")
			addItem(ModItems.WEB_PORT, "Web Port")
			addItem(ModItems.HOPPING_SPIDER, "Hopping Spider")
			addItem(ModItems.GREEN_DYEBERRY, "Green Dyeberry")
			addItem(ModItems.BLUE_DYEBERRY, "Blue Dyeberry")
			addItem(ModItems.RED_DYEBERRY, "Red Dyeberry")
			addItem(ModItems.YELLOW_DYEBERRY, "Yellow Dyeberry")
			addItem(ModItems.MAGENTA_DYEBERRY, "Magenta Dyeberry")
			addItem(ModItems.CYAN_DYEBERRY, "Cyan Dyeberry")
			addItem(ModItems.AARONBERRY, "Aaronberry")
			addItem(ModItems.SPARKPOLLEN_POUCH, "Sparkpollen Pouch")
			addItem(ModItems.TARDIGRADE, "Tardigrade")
			addItem(ModItems.SNAIL_SHIELD, "Snail Shield")
			addItem(ModItems.SNAIL_SHELL_FRAGMENT, "Snail Shell Fragment")
			addItem(ModItems.SNAIL_SLIME, "Snail Slime")
			addItem(ModItems.PEARL, "Pearl")
			addItem(ModItems.SCOOCHWORM_SPAWN_EGG, "Scoochworm Spawn Egg")
			addItem(ModItems.ITEM_FILTERFEEDER_SPAWN_EGG, "Item Filterfeeder Spawn Egg")
			addItem(ModItems.FLUID_FILTERFEEDER_SPAWN_EGG, "Fluid Filterfeeder Spawn Egg")
			addItem(ModItems.PUNCHPRAWN_SPAWN_EGG, "Punchprawn Spawn Egg")
			addItem(ModItems.SPROUTSNAIL_SPAWN_EGG, "Sproutsnail Spawn Egg")
		}
	}
}