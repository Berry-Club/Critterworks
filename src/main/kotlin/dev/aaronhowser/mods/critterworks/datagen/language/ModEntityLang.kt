package dev.aaronhowser.mods.critterworks.datagen.language

import dev.aaronhowser.mods.critterworks.registry.ModEntityTypes

object ModEntityLang {

	const val ITEM_FILTERFEEDER = "entity.critterworks.filterfeeder.item"
	const val FLUID_FILTERFEEDER = "entity.critterworks.filterfeeder.fluid"
	const val EVIL_SNAIL = "entity.critterworks.evilsnail"
	const val EVIL_SNAIL_DEATH = "death.attack.evilSnail"

	fun add(provider: ModLanguageProvider) {
		provider.addEntityType(ModEntityTypes.SCOOCHWORM, "Scoochworm")
		provider.addEntityType(ModEntityTypes.SCOOCHWORM_PART, "Scoochworm Segment")
		provider.addEntityType(ModEntityTypes.FILTERFEEDER, "Filterfeeder")
		provider.addEntityType(ModEntityTypes.PUNCHPRAWN, "Punchprawn")
		provider.addEntityType(ModEntityTypes.SPROUTSNAIL, "Sproutsnail")

		provider.add(ITEM_FILTERFEEDER, "Item Filterfeeder")
		provider.add(FLUID_FILTERFEEDER, "Fluid Filterfeeder")
		provider.add(EVIL_SNAIL, "The Snail That Kills %s")
		provider.add(EVIL_SNAIL_DEATH, $$"%1$s was killed by the Snail That Kills %1$s")
	}
}