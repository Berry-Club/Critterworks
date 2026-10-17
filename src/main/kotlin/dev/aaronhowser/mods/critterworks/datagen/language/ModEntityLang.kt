package dev.aaronhowser.mods.critterworks.datagen.language

import dev.aaronhowser.mods.critterworks.registry.ModEntityTypes

object ModEntityLang {

	const val ITEM_FILTERFEEDER = "entity.critterworks.filterfeeder.item"
	const val FLUID_FILTERFEEDER = "entity.critterworks.filterfeeder.fluid"

	fun add(provider: ModLanguageProvider) {
		provider.addEntityType(ModEntityTypes.SCOOCHWORM, "Scoochworm")
		provider.add(ITEM_FILTERFEEDER, "Item Filterfeeder")
		provider.add(FLUID_FILTERFEEDER, "Fluid Filterfeeder")
	}
}