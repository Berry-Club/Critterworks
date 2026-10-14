package dev.aaronhowser.mods.critterworks.registry

import dev.aaronhowser.mods.critterworks.Critterworks
import dev.aaronhowser.mods.critterworks.recipe.crafting.ApplyTardigradeRecipe
import dev.aaronhowser.mods.critterworks.recipe.crafting.RemoveTardigradeRecipe
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister
import java.util.function.Supplier

object ModRecipeSerializers {

	val RECIPE_SERIALIZER_REGISTRY: DeferredRegister<RecipeSerializer<*>> =
		DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, Critterworks.MOD_ID)

	val APPLY_TARDIGRADE: DeferredHolder<RecipeSerializer<*>, RecipeSerializer<*>> =
		RECIPE_SERIALIZER_REGISTRY.register("apply_tardigrade", Supplier {
			SimpleCraftingRecipeSerializer(::ApplyTardigradeRecipe)
		})

	val REMOVE_TARDIGRADE: DeferredHolder<RecipeSerializer<*>, RecipeSerializer<*>> =
		RECIPE_SERIALIZER_REGISTRY.register("remove_tardigrade", Supplier {
			RemoveTardigradeRecipe.Serializer()
		})
}