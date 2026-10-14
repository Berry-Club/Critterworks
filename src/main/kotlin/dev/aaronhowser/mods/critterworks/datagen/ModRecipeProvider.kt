package dev.aaronhowser.mods.critterworks.datagen

import dev.aaronhowser.mods.aaron.datagen.AaronRecipeProvider
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.asIngredient
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.getAsStack
import dev.aaronhowser.mods.critterworks.Critterworks
import dev.aaronhowser.mods.critterworks.datagen.tag.ModItemTagsProvider
import dev.aaronhowser.mods.critterworks.recipe.crafting.ApplyTardigradeRecipe
import dev.aaronhowser.mods.critterworks.recipe.crafting.RemoveTardigradeRecipe
import dev.aaronhowser.mods.critterworks.registry.ModBlocks
import dev.aaronhowser.mods.critterworks.registry.ModItems
import net.minecraft.core.HolderLookup
import net.minecraft.data.PackOutput
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.data.recipes.SpecialRecipeBuilder
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import net.minecraft.world.item.alchemy.Potions
import net.minecraft.world.level.block.Block
import net.neoforged.neoforge.common.Tags
import net.neoforged.neoforge.registries.DeferredBlock
import net.neoforged.neoforge.registries.DeferredItem
import java.util.concurrent.CompletableFuture

class ModRecipeProvider(
	output: PackOutput,
	lookupProvider: CompletableFuture<HolderLookup.Provider>
) : AaronRecipeProvider(output, lookupProvider) {

	private fun modLoc(name: String) = Critterworks.modResource(name)

	override fun buildRecipes(recipeOutput: RecipeOutput, holderLookup: HolderLookup.Provider) {
		buildShapedRecipes(recipeOutput, holderLookup)
		buildShapelessRecipes(recipeOutput, holderLookup)
		buildNamedRecipes(recipeOutput, holderLookup)
		buildTardigradeRecipes(recipeOutput)
	}

	private fun buildTardigradeRecipes(recipeOutput: RecipeOutput) {
		SpecialRecipeBuilder
			.special(::ApplyTardigradeRecipe)
			.save(recipeOutput, modLoc("apply_tardigrade"))

		val poisonIngredient = Potions.POISON.getAsStack().asIngredient()

		recipeOutput.accept(
			modLoc("remove_tardigrade"),
			RemoveTardigradeRecipe(poisonIngredient),
			null
		)
	}

	private fun buildShapelessRecipes(recipeOutput: RecipeOutput, holderLookup: HolderLookup.Provider) {
		shapelessRecipe(
			ModBlocks.STEM_ENCASED_COMPARATOR,
			listOf(
				ModBlocks.SCOOCHSTEM.asIngredient(),
				Items.COMPARATOR.asIngredient()
			)
		).save(recipeOutput)

		shapelessRecipe(
			ModBlocks.SCOOCHWORM_DEPOT,
			listOf(
				ModItemTagsProvider.SCOOCHSTEM_LOGS.asIngredient(),
				Tags.Items.FENCE_GATES_WOODEN.asIngredient()
			)
		).save(recipeOutput)

		fun coloredScoochstem(dyeBerry: DeferredItem<out Item>, output: DeferredBlock<out Block>) {
			shapelessRecipe(
				output,
				listOf(
					dyeBerry.asIngredient(),
					ModBlocks.SCOOCHSTEM.asIngredient()
				)
			).save(recipeOutput)
		}

		coloredScoochstem(ModItems.RED_DYEBERRY, ModBlocks.RED_SCOOCHSTEM)
		coloredScoochstem(ModItems.BLUE_DYEBERRY, ModBlocks.BLUE_SCOOCHSTEM)
		coloredScoochstem(ModItems.CYAN_DYEBERRY, ModBlocks.CYAN_SCOOCHSTEM)
		coloredScoochstem(ModItems.YELLOW_DYEBERRY, ModBlocks.YELLOW_SCOOCHSTEM)
		coloredScoochstem(ModItems.GREEN_DYEBERRY, ModBlocks.GREEN_SCOOCHSTEM)
		coloredScoochstem(ModItems.MAGENTA_DYEBERRY, ModBlocks.MAGENTA_SCOOCHSTEM)
	}

	private fun buildShapedRecipes(recipeOutput: RecipeOutput, holderLookup: HolderLookup.Provider) {
		shapedRecipe(
			ModBlocks.SCOOCHSTEM_WOOD,
			4,
			"WW,WW",
			mapOf(
				'W' to ModBlocks.SCOOCHSTEM.asIngredient()
			)
		).save(recipeOutput)

		shapedRecipe(
			ModItems.LOCKBOX.toStack(),
			"WWW,WIW,WWW",
			mapOf(
				'W' to ModItemTagsProvider.SCOOCHSTEM_LOGS.asIngredient(),
				'I' to Tags.Items.NUGGETS_IRON.asIngredient()
			)
		).save(recipeOutput)

		shapedRecipe(
			ModItems.SCOOCHWORM_GPS.toStack(),
			" E ,WCW, E ",
			mapOf(
				'E' to Tags.Items.ENDER_PEARLS.asIngredient(),
				'W' to ModItemTagsProvider.SCOOCHSTEM_LOGS.asIngredient(),
				'C' to Items.COMPASS.asIngredient()
			)
		).save(recipeOutput)

		shapedRecipe(
			ModItems.CRITTER_CAGE.toStack(),
			"III,IRI,WWW",
			mapOf(
				'I' to Items.IRON_BARS.asIngredient(),
				'W' to ModItemTagsProvider.SCOOCHSTEM_LOGS.asIngredient(),
				'R' to Tags.Items.DUSTS_REDSTONE.asIngredient()
			)
		).save(recipeOutput)

		shapedRecipe(
			ModItems.ARTIFICIAL_SPINNERETS.toStack(),
			"I I,TGT, S ",
			mapOf(
				'I' to Tags.Items.INGOTS_IRON.asIngredient(),
				'T' to Tags.Items.STRINGS.asIngredient(),
				'G' to Items.GLASS_BOTTLE.asIngredient(),
				'S' to Tags.Items.SLIME_BALLS.asIngredient()
			)
		).save(recipeOutput)

		shapedRecipe(
			ModItems.WEB_PORT.toStack(),
			"ISI,SHS,ISI",
			mapOf(
				'I' to Tags.Items.NUGGETS_IRON.asIngredient(),
				'S' to Tags.Items.STRINGS.asIngredient(),
				'H' to Items.HOPPER.asIngredient()
			)
		).save(recipeOutput)

		shapedRecipe(
			ModItems.WEB_PATHFINDER.toStack(),
			" A ,SCS, R ",
			mapOf(
				'A' to Items.AMETHYST_SHARD.asIngredient(),
				'S' to Tags.Items.STRINGS.asIngredient(),
				'C' to Items.COMPASS.asIngredient(),
				'R' to Tags.Items.DUSTS_REDSTONE.asIngredient()
			)
		).save(recipeOutput)

		shapedRecipe(
			ModItems.SPARKPOLLEN_POUCH.toStack(),
			"SGS,RRR",
			mapOf(
				'S' to Tags.Items.STRINGS.asIngredient(),
				'R' to Items.RABBIT_HIDE.asIngredient(),
				'G' to Tags.Items.DUSTS_GLOWSTONE.asIngredient()
			)
		).save(recipeOutput)

		shapedRecipe(
			ModItems.ITEM_FILTER.toStack(),
			"SPS,PCP, P ",
			mapOf(
				'S' to Tags.Items.STRINGS.asIngredient(),
				'P' to Items.PAPER.asIngredient(),
				'C' to Items.COMPARATOR.asIngredient()
			)
		).save(recipeOutput)
	}

	private fun buildNamedRecipes(recipeOutput: RecipeOutput, holderLookup: HolderLookup.Provider) {
		shapelessRecipe(
			Items.RED_DYE,
			listOf(ModItems.RED_DYEBERRY.asIngredient())
		).save(recipeOutput, modLoc("red_dye_from_dyeberry"))

		shapelessRecipe(
			Items.BLUE_DYE,
			listOf(ModItems.BLUE_DYEBERRY.asIngredient())
		).save(recipeOutput, modLoc("blue_dye_from_dyeberry"))

		shapelessRecipe(
			Items.GREEN_DYE,
			listOf(ModItems.GREEN_DYEBERRY.asIngredient())
		).save(recipeOutput, modLoc("green_dye_from_dyeberry"))

		shapelessRecipe(
			Items.CYAN_DYE,
			listOf(ModItems.CYAN_DYEBERRY.asIngredient())
		).save(recipeOutput, modLoc("cyan_dye_from_dyeberry"))

		shapelessRecipe(
			Items.YELLOW_DYE,
			listOf(ModItems.YELLOW_DYEBERRY.asIngredient())
		).save(recipeOutput, modLoc("yellow_dye_from_dyeberry"))

		shapelessRecipe(
			Items.MAGENTA_DYE,
			listOf(ModItems.MAGENTA_DYEBERRY.asIngredient())
		).save(recipeOutput, modLoc("magenta_dye_from_dyeberry"))

		shapelessRecipe(
			Items.ROTTEN_FLESH,
			listOf(ModItems.AARONBERRY.asIngredient())
		).save(recipeOutput, modLoc("rotten_flesh_from_aaronberry"))

		shapelessRecipe(
			Items.APPLE,
			9,
			listOf(ModBlocks.APPLE_SLICE.asIngredient())
		).save(recipeOutput, modLoc("apples_from_apple_slice"))
	}

}