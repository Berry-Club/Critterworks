package dev.aaronhowser.mods.critterworks.recipe.crafting

import dev.aaronhowser.mods.aaron.misc.AaronExtensions.asIngredient
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.setUnit
import dev.aaronhowser.mods.critterworks.registry.ModDataComponents
import dev.aaronhowser.mods.critterworks.registry.ModItems
import dev.aaronhowser.mods.critterworks.registry.ModRecipeSerializers
import net.minecraft.core.HolderLookup
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.*
import net.minecraft.world.level.Level

class ApplyTardigradeRecipe(
	craftingCategory: CraftingBookCategory = CraftingBookCategory.MISC
) : CustomRecipe(craftingCategory) {

	override fun matches(input: CraftingInput, level: Level): Boolean {
		return findTargetStack(input) != null
	}

	override fun assemble(input: CraftingInput, registries: HolderLookup.Provider): ItemStack {
		val targetStack = findTargetStack(input) ?: return ItemStack.EMPTY
		val result = targetStack.copyWithCount(1)

		result.setUnit(ModDataComponents.HAS_TARDIGRADE)

		return result
	}

	override fun canCraftInDimensions(width: Int, height: Int): Boolean {
		return width * height >= 2
	}

	override fun getSerializer(): RecipeSerializer<*> {
		return ModRecipeSerializers.APPLY_TARDIGRADE.get()
	}

	private fun findTargetStack(input: CraftingInput): ItemStack? {
		var foundTardigrade = false
		var targetStack: ItemStack? = null

		for (itemStack in input.items()) {
			if (itemStack.isEmpty) continue

			if (TARDIGRADE_INGREDIENT.test(itemStack)) {
				if (foundTardigrade) return null

				foundTardigrade = true
				continue
			}

			if (targetStack != null || itemStack.has(ModDataComponents.HAS_TARDIGRADE)) return null

			targetStack = itemStack
		}

		if (!foundTardigrade) return null

		return targetStack
	}

	companion object {
		val TARDIGRADE_INGREDIENT: Ingredient = ModItems.TARDIGRADE.asIngredient()
	}
}