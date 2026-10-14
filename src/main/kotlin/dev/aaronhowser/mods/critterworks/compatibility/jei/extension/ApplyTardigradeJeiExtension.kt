package dev.aaronhowser.mods.critterworks.compatibility.jei.extension

import dev.aaronhowser.mods.aaron.misc.AaronExtensions.setUnit
import dev.aaronhowser.mods.critterworks.recipe.crafting.ApplyTardigradeRecipe
import dev.aaronhowser.mods.critterworks.registry.ModDataComponents
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder
import mezz.jei.api.gui.ingredient.ICraftingGridHelper
import mezz.jei.api.recipe.IFocusGroup
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.RecipeHolder

class ApplyTardigradeJeiExtension(
	targetStacks: List<ItemStack>
) : ICraftingCategoryExtension<ApplyTardigradeRecipe> {

	private val inputStacks = mutableListOf<ItemStack>()
	private val outputStacks = mutableListOf<ItemStack?>()

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
		val inputs = listOf(
			ApplyTardigradeRecipe.TARDIGRADE_INGREDIENT.items.toList(),
			inputStacks
		)
		val inputSlots = craftingGridHelper.createAndSetInputs(builder, inputs, 0, 0)
		val outputSlot = craftingGridHelper.createAndSetOutputs(builder, outputStacks)

		builder.createFocusLink(inputSlots[1], outputSlot)
		builder.setShapeless()
	}
}