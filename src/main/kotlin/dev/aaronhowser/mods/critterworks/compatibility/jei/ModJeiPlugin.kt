package dev.aaronhowser.mods.critterworks.compatibility.jei

import dev.aaronhowser.mods.critterworks.Critterworks
import dev.aaronhowser.mods.critterworks.compatibility.jei.extension.ApplyTardigradeJeiExtension
import dev.aaronhowser.mods.critterworks.compatibility.jei.extension.RemoveTardigradeJeiExtension
import dev.aaronhowser.mods.critterworks.recipe.crafting.ApplyTardigradeRecipe
import dev.aaronhowser.mods.critterworks.recipe.crafting.RemoveTardigradeRecipe
import dev.aaronhowser.mods.critterworks.registry.ModDataComponents
import dev.aaronhowser.mods.critterworks.registry.ModItems
import mezz.jei.api.IModPlugin
import mezz.jei.api.JeiPlugin
import mezz.jei.api.registration.IVanillaCategoryExtensionRegistration
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack

@JeiPlugin
class ModJeiPlugin : IModPlugin {

	override fun getPluginUid(): ResourceLocation {
		return Critterworks.modResource("jei_plugin")
	}

	override fun registerVanillaCategoryExtensions(registration: IVanillaCategoryExtensionRegistration) {
		val targetStacks = getTardigradeTargetStacks(registration)
		val craftingCategory = registration.craftingCategory

		craftingCategory.addExtension(
			ApplyTardigradeRecipe::class.java,
			ApplyTardigradeJeiExtension(targetStacks)
		)
		craftingCategory.addExtension(
			RemoveTardigradeRecipe::class.java,
			RemoveTardigradeJeiExtension(targetStacks)
		)
	}

	private fun getTardigradeTargetStacks(
		registration: IVanillaCategoryExtensionRegistration
	): List<ItemStack> {
		val allItemStacks = registration.jeiHelpers.ingredientManager.allItemStacks
		val targetStacks = mutableListOf<ItemStack>()

		for (itemStack in allItemStacks) {
			if (itemStack.isEmpty) continue
			if (itemStack.item === ModItems.TARDIGRADE.get()) continue
			if (itemStack.has(ModDataComponents.HAS_TARDIGRADE)) continue

			targetStacks.add(itemStack.copyWithCount(1))
		}

		return targetStacks
	}
}