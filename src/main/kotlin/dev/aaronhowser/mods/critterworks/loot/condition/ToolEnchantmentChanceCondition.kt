package dev.aaronhowser.mods.critterworks.loot.condition

import com.google.common.collect.ImmutableSet
import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import dev.aaronhowser.mods.critterworks.registry.ModLootItemConditionTypes
import net.minecraft.core.Holder
import net.minecraft.world.item.enchantment.Enchantment
import net.minecraft.world.item.enchantment.LevelBasedValue
import net.minecraft.world.level.storage.loot.LootContext
import net.minecraft.world.level.storage.loot.parameters.LootContextParam
import net.minecraft.world.level.storage.loot.parameters.LootContextParams
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType

data class ToolEnchantmentChanceCondition(
	val unenchantedChance: Float,
	val enchantedChance: LevelBasedValue,
	val enchantment: Holder<Enchantment>
) : LootItemCondition {

	override fun getType(): LootItemConditionType {
		return ModLootItemConditionTypes.TOOL_ENCHANTMENT_CHANCE.get()
	}

	override fun getReferencedContextParams(): Set<LootContextParam<*>> {
		return ImmutableSet.of(LootContextParams.TOOL)
	}

	override fun test(context: LootContext): Boolean {
		val tool = context.getParam(LootContextParams.TOOL)
		val enchantmentLevel = tool.getEnchantmentLevel(enchantment)

		val chance = if (enchantmentLevel > 0) {
			enchantedChance.calculate(enchantmentLevel)
		} else {
			unenchantedChance
		}

		return context.random.nextFloat() < chance
	}

	companion object {
		val CODEC: MapCodec<ToolEnchantmentChanceCondition> = RecordCodecBuilder.mapCodec { instance ->
			instance.group(
				Codec.floatRange(0f, 1f)
					.fieldOf("unenchanted_chance")
					.forGetter(ToolEnchantmentChanceCondition::unenchantedChance),
				LevelBasedValue.CODEC
					.fieldOf("enchanted_chance")
					.forGetter(ToolEnchantmentChanceCondition::enchantedChance),
				Enchantment.CODEC
					.fieldOf("enchantment")
					.forGetter(ToolEnchantmentChanceCondition::enchantment)
			).apply(instance, ::ToolEnchantmentChanceCondition)
		}
	}
}