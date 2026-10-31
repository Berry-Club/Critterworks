package dev.aaronhowser.mods.critterworks.compatibility.jei

import dev.aaronhowser.mods.critterworks.Critterworks
import dev.aaronhowser.mods.critterworks.recipe.crafting.RemoveTardigradeRecipe
import dev.aaronhowser.mods.critterworks.registry.ModDataComponents
import mezz.jei.api.ingredients.ITypedIngredient
import mezz.jei.api.recipe.advanced.ISimpleRecipeManagerPlugin
import net.minecraft.client.Minecraft
import net.minecraft.world.item.crafting.CraftingRecipe
import net.minecraft.world.item.crafting.RecipeHolder

class RemoveTardigradeRecipeManagerPlugin : ISimpleRecipeManagerPlugin<RecipeHolder<CraftingRecipe>> {

	override fun isHandledInput(input: ITypedIngredient<*>): Boolean {
		val itemStack = input.itemStack.orElse(null) ?: return false

		return itemStack.has(ModDataComponents.HAS_TARDIGRADE)
	}

	override fun isHandledOutput(output: ITypedIngredient<*>): Boolean {
		return false
	}

	override fun getRecipesForInput(input: ITypedIngredient<*>): List<RecipeHolder<CraftingRecipe>> {
		if (!isHandledInput(input)) return emptyList()

		return getRemovalRecipe()
	}

	override fun getRecipesForOutput(output: ITypedIngredient<*>): List<RecipeHolder<CraftingRecipe>> {
		return emptyList()
	}

	override fun getAllRecipes(): List<RecipeHolder<CraftingRecipe>> {
		return emptyList()
	}

	private fun getRemovalRecipe(): List<RecipeHolder<CraftingRecipe>> {
		val level = Minecraft.getInstance().level ?: return emptyList()
		val recipeHolder = level.recipeManager
			.byKey(REMOVE_TARDIGRADE_RECIPE_ID)
			.orElse(null)
			?: return emptyList()

		if (recipeHolder.value !is RemoveTardigradeRecipe) return emptyList()

		@Suppress("UNCHECKED_CAST")
		return listOf(recipeHolder as RecipeHolder<CraftingRecipe>)
	}

	companion object {
		private val REMOVE_TARDIGRADE_RECIPE_ID = Critterworks.modResource("remove_tardigrade")
	}
}