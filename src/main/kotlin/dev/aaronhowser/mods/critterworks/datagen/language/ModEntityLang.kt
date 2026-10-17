package dev.aaronhowser.mods.critterworks.datagen.language

import dev.aaronhowser.mods.critterworks.registry.ModEntityTypes

object ModEntityLang {

	fun add(provider: ModLanguageProvider) {
		provider.addEntityType(ModEntityTypes.FILTERFEEDER, "Item Filterfeeder")
		provider.addEntityType(ModEntityTypes.SCOOCHWORM, "Scoochworm")
	}
}