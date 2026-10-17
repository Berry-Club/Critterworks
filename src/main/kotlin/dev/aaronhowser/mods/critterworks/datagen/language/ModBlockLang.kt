package dev.aaronhowser.mods.critterworks.datagen.language

import dev.aaronhowser.mods.critterworks.registry.ModBlocks

object ModBlockLang {

	fun add(provider: ModLanguageProvider) {
		provider.apply {
			addBlock(ModBlocks.SCOOCHSTEM, "Scoochstem")
			addBlock(ModBlocks.STEM_ENCASED_COMPARATOR, "Stem-Encased Comparator")
			addBlock(ModBlocks.SCOOCHSTEM_WOOD, "Scoochstem Wood")
			addBlock(ModBlocks.SCOOCHWORM_DEPOT, "Scoochworm Depot")
			addBlock(ModBlocks.APPLE_SLICE, "Apple Slice")
			addBlock(ModBlocks.HOPPING_SPIDER_NEST, "Hopping Spider Nest")
			addBlock(ModBlocks.CRITTER_CAGE, "Critter Cage")
			addBlock(ModBlocks.DYEBERRY_VINES, "Dyeberry Vines")
			addBlock(ModBlocks.DYEBERRY_VINES_PLANT, "Dyeberry Vines Plant")
			addBlock(ModBlocks.SPARKBUG_BUSH, "Sparkbug Bush")
			addBlock(ModBlocks.ITEM_FILTERFEEDER, "Item Filterfeeder")

			addBlock(ModBlocks.GREEN_SCOOCHSTEM, "Green Scoochstem")
			addBlock(ModBlocks.BLUE_SCOOCHSTEM, "Blue Scoochstem")
			addBlock(ModBlocks.RED_SCOOCHSTEM, "Red Scoochstem")
			addBlock(ModBlocks.YELLOW_SCOOCHSTEM, "Yellow Scoochstem")
			addBlock(ModBlocks.MAGENTA_SCOOCHSTEM, "Magenta Scoochstem")
			addBlock(ModBlocks.CYAN_SCOOCHSTEM, "Cyan Scoochstem")

		}
	}
}