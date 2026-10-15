package dev.aaronhowser.mods.critterworks.registry

import dev.aaronhowser.mods.critterworks.Critterworks
import dev.aaronhowser.mods.critterworks.loot.condition.ToolEnchantmentChanceCondition
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister
import java.util.function.Supplier

object ModLootItemConditionTypes {

	val LOOT_CONDITION_TYPE_REGISTRY: DeferredRegister<LootItemConditionType> =
		DeferredRegister.create(BuiltInRegistries.LOOT_CONDITION_TYPE, Critterworks.MOD_ID)

	val TOOL_ENCHANTMENT_CHANCE: DeferredHolder<LootItemConditionType, LootItemConditionType> =
		LOOT_CONDITION_TYPE_REGISTRY.register("tool_enchantment_chance", Supplier {
			LootItemConditionType(ToolEnchantmentChanceCondition.CODEC)
		})
}