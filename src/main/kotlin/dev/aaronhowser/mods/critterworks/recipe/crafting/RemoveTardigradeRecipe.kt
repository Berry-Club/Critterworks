package dev.aaronhowser.mods.critterworks.recipe.crafting

import com.mojang.serialization.MapCodec
import dev.aaronhowser.mods.critterworks.registry.ModDataComponents
import dev.aaronhowser.mods.critterworks.registry.ModRecipeSerializers
import net.minecraft.core.HolderLookup
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.*
import net.minecraft.world.level.Level

class RemoveTardigradeRecipe(
	val removalIngredient: Ingredient,
	craftingCategory: CraftingBookCategory = CraftingBookCategory.MISC
) : CustomRecipe(craftingCategory) {

	override fun matches(input: CraftingInput, level: Level): Boolean {
		return findTargetStack(input) != null
	}

	override fun assemble(input: CraftingInput, registries: HolderLookup.Provider): ItemStack {
		val targetStack = findTargetStack(input) ?: return ItemStack.EMPTY
		val result = targetStack.copyWithCount(1)

		result.remove(ModDataComponents.HAS_TARDIGRADE)

		return result
	}

	override fun canCraftInDimensions(width: Int, height: Int): Boolean {
		return width * height >= 2
	}

	override fun getSerializer(): RecipeSerializer<*> {
		return ModRecipeSerializers.REMOVE_TARDIGRADE.get()
	}

	private fun findTargetStack(input: CraftingInput): ItemStack? {
		var firstStack: ItemStack? = null
		var secondStack: ItemStack? = null

		for (itemStack in input.items()) {
			if (itemStack.isEmpty) continue

			if (firstStack == null) {
				firstStack = itemStack
				continue
			}

			if (secondStack != null) return null

			secondStack = itemStack
		}

		if (firstStack == null || secondStack == null) return null

		if (removalIngredient.test(firstStack) && secondStack.has(ModDataComponents.HAS_TARDIGRADE)) {
			return secondStack
		}

		if (removalIngredient.test(secondStack) && firstStack.has(ModDataComponents.HAS_TARDIGRADE)) {
			return firstStack
		}

		return null
	}

	class Serializer : RecipeSerializer<RemoveTardigradeRecipe> {

		override fun codec(): MapCodec<RemoveTardigradeRecipe> {
			return CODEC
		}

		override fun streamCodec(): StreamCodec<RegistryFriendlyByteBuf, RemoveTardigradeRecipe> {
			return STREAM_CODEC
		}

		companion object {
			val CODEC: MapCodec<RemoveTardigradeRecipe> =
				Ingredient.CODEC_NONEMPTY
					.fieldOf("removal_ingredient")
					.xmap(::RemoveTardigradeRecipe, RemoveTardigradeRecipe::removalIngredient)

			val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, RemoveTardigradeRecipe> =
				Ingredient.CONTENTS_STREAM_CODEC
					.map(::RemoveTardigradeRecipe, RemoveTardigradeRecipe::removalIngredient)
		}
	}
}