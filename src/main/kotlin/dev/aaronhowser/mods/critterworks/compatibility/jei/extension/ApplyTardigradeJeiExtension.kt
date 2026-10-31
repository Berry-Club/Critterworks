package dev.aaronhowser.mods.critterworks.compatibility.jei.extension

import dev.aaronhowser.mods.aaron.misc.AaronExtensions.setUnit
import dev.aaronhowser.mods.critterworks.recipe.crafting.ApplyTardigradeRecipe
import dev.aaronhowser.mods.critterworks.registry.ModDataComponents
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder
import mezz.jei.api.gui.ingredient.ICraftingGridHelper
import mezz.jei.api.recipe.IFocusGroup
import mezz.jei.api.recipe.RecipeIngredientRole
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.RecipeHolder

class ApplyTardigradeJeiExtension(
	targetStacks: List<ItemStack>
) : ICraftingCategoryExtension<ApplyTardigradeRecipe> {

	private val inputStacks = mutableListOf<ItemStack>()
	private val outputStacks = mutableListOf<ItemStack>()

	init {
		for (targetStack in targetStacks) {
			val inputStack = targetStack.copyWithCount(1)
			val outputStack = inputStack.copy()
			outputStack.setUnit(ModDataComponents.HAS_TARDIGRADE)

			inputStacks.add(inputStack)
			outputStacks.add(outputStack)
		}
	}

	override fun setRecipe(
		recipeHolder: RecipeHolder<ApplyTardigradeRecipe>,
		builder: IRecipeLayoutBuilder,
		craftingGridHelper: ICraftingGridHelper,
		focuses: IFocusGroup
	) {
		builder
			.addSlot(RecipeIngredientRole.INPUT, 1, 1)
			.setStandardSlotBackground()
			.addIngredients(ApplyTardigradeRecipe.TARDIGRADE_INGREDIENT)

		val targetInputSlot = builder
			.addSlot(RecipeIngredientRole.RENDER_ONLY, 19, 1)
			.setStandardSlotBackground()
			.addItemStacks(inputStacks)

		addEmptyCraftingSlots(builder)

		val outputSlot = builder
			.addSlot(RecipeIngredientRole.RENDER_ONLY, 95, 19)
			.setOutputSlotBackground()
			.addItemStacks(outputStacks)

		builder.createFocusLink(targetInputSlot, outputSlot)
		builder.setShapeless()
	}

	private fun addEmptyCraftingSlots(builder: IRecipeLayoutBuilder) {
		for (slotIndex in 2 until 9) {
			val column = slotIndex % 3
			val row = slotIndex / 3

			builder
				.addSlot(RecipeIngredientRole.RENDER_ONLY, column * 18 + 1, row * 18 + 1)
				.setStandardSlotBackground()
		}
	}
}