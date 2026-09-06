package dev.aaronhowser.mods.critterworks.datagen.language

import dev.aaronhowser.mods.critterworks.registry.ModBlocks

object ModBlockLang {

	fun add(provider: ModLanguageProvider) {
		provider.addBlock(ModBlocks.SCOOCHSTEM, "Scoochstem")
		provider.addBlock(ModBlocks.STEM_ENCASED_COMPARATOR, "Stem-Encased Comparator")
		provider.addBlock(ModBlocks.SCOOCHSTEM_WOOD, "Scoochstem Wood")
		provider.addBlock(ModBlocks.SCOOCHWORM_DEPOT, "Scoochworm Depot")
		provider.addBlock(ModBlocks.APPLE_SLICE, "Apple Slice")
		provider.addBlock(ModBlocks.HOPPING_SPIDER_NEST, "Hopping Spider Nest")

		provider.addBlock(ModBlocks.GREEN_SCOOCHSTEM, "Green Scoochstem")
		provider.addBlock(ModBlocks.BLUE_SCOOCHSTEM, "Blue Scoochstem")
		provider.addBlock(ModBlocks.RED_SCOOCHSTEM, "Red Scoochstem")
		provider.addBlock(ModBlocks.YELLOW_SCOOCHSTEM, "Yellow Scoochstem")
		provider.addBlock(ModBlocks.MAGENTA_SCOOCHSTEM, "Magenta Scoochstem")
		provider.addBlock(ModBlocks.CYAN_SCOOCHSTEM, "Cyan Scoochstem")
	}
}