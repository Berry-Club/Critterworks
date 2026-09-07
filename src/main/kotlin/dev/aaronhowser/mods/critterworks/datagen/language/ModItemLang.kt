package dev.aaronhowser.mods.critterworks.datagen.language

import dev.aaronhowser.mods.critterworks.registry.ModCreativeModeTabs
import dev.aaronhowser.mods.critterworks.registry.ModItems

object ModItemLang {

	fun add(provider: ModLanguageProvider) {
		provider.apply {
			addItem(ModItems.LOCKBOX, "Lockbox")
			addItem(ModItems.SCOOCHWORM_GPS, "Scoochworm GPS")
			addItem(ModItems.ARTIFICIAL_SPINNERETS, "Artificial Spinnerets")
			addItem(ModItems.WEB_PATHFINDER, "Web Pathfinder")
			addItem(ModItems.ITEM_FILTER, "Item Filter")
			addItem(ModItems.WEB_PORT, "Web Port")
			addItem(ModItems.HOPPING_SPIDER, "Hopping Spider")
			addItem(ModItems.GREEN_DYEBERRY, "Green Dyeberry")
			addItem(ModItems.BLUE_DYEBERRY, "Blue Dyeberry")
			addItem(ModItems.RED_DYEBERRY, "Red Dyeberry")
			addItem(ModItems.YELLOW_DYEBERRY, "Yellow Dyeberry")
			addItem(ModItems.MAGENTA_DYEBERRY, "Magenta Dyeberry")
			addItem(ModItems.CYAN_DYEBERRY, "Cyan Dyeberry")
			addItem(ModItems.AARONBERRY, "Aaronberry")
			addItem(ModItems.SCOOCHWORM_SPAWN_EGG, "Scoochworm Spawn Egg")
			add(ModCreativeModeTabs.CREATIVE_TAB_TRANSLATION_KEY, "Critterworks")
		}
	}
}
