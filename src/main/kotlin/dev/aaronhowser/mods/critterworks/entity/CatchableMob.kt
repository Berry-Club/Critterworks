package dev.aaronhowser.mods.critterworks.entity

import dev.aaronhowser.mods.critterworks.registry.ModDataComponents
import net.minecraft.core.component.DataComponents
import net.minecraft.util.Mth
import net.minecraft.world.entity.Mob
import net.minecraft.world.item.ItemStack
import kotlin.math.roundToInt

interface CatchableMob {

	fun getCaughtItemStack(): ItemStack

	fun loadItemData(stack: ItemStack) {
		val entity = getAsMob()
		entity.setPersistenceRequired()

		val entityHealth = stack.get(ModDataComponents.ENTITY_HEALTH)
		if (entityHealth != null) {
			entity.health = entityHealth
		}

		val customName = stack.get(DataComponents.CUSTOM_NAME)
		if (customName != null) {
			entity.customName = customName
		}
	}

	fun saveItemData(stack: ItemStack) {
		val entity = getAsMob()
		stack.set(ModDataComponents.ENTITY_HEALTH, entity.health)

		val customName = entity.customName
		if (customName != null) {
			stack.set(DataComponents.CUSTOM_NAME, customName)
		}
	}

	private fun getAsMob(): Mob {
		check(this is Mob) { "Catchable entities must be mobs" }
		return this
	}

	companion object {
		private const val ITEM_BAR_WIDTH = 13

		fun isHealthBarVisible(stack: ItemStack, maximumHealth: Float): Boolean {
			return getHealth(stack, maximumHealth) < maximumHealth
		}

		fun getHealthBarWidth(stack: ItemStack, maximumHealth: Float): Int {
			val healthPercentage = getHealthPercentage(stack, maximumHealth)
			return (healthPercentage * ITEM_BAR_WIDTH).roundToInt()
		}

		fun getHealthBarColor(stack: ItemStack, maximumHealth: Float): Int {
			val healthPercentage = getHealthPercentage(stack, maximumHealth)
			return Mth.hsvToRgb(healthPercentage / 3f, 1f, 1f)
		}

		private fun getHealthPercentage(stack: ItemStack, maximumHealth: Float): Float {
			val health = getHealth(stack, maximumHealth)
			return Mth.clamp(health / maximumHealth, 0f, 1f)
		}

		private fun getHealth(stack: ItemStack, maximumHealth: Float): Float {
			return stack.getOrDefault(ModDataComponents.ENTITY_HEALTH, maximumHealth)
		}
	}

}