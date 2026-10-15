package dev.aaronhowser.mods.critterworks.datagen.loot

import dev.aaronhowser.mods.aaron.datagen.AaronLootTableDsl
import dev.aaronhowser.mods.critterworks.Critterworks
import dev.aaronhowser.mods.critterworks.loot.condition.ToolEnchantmentChanceCondition
import dev.aaronhowser.mods.critterworks.registry.ModItems
import net.minecraft.core.HolderLookup
import net.minecraft.core.registries.Registries
import net.minecraft.data.loot.LootTableSubProvider
import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.enchantment.Enchantments
import net.minecraft.world.item.enchantment.LevelBasedValue
import net.minecraft.world.level.storage.loot.LootTable
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition
import java.util.function.BiConsumer

class ModOtherLootTablesSubProvider(
	private val registries: HolderLookup.Provider
) : LootTableSubProvider {

	override fun generate(output: BiConsumer<ResourceKey<LootTable>, LootTable.Builder?>) {
		output.accept(
			TARDIGRADE_FROM_MOSS,
			AaronLootTableDsl.table {
				pool {
					item(ModItems.TARDIGRADE.get())
					condition(LootItemCondition.Builder(::createTardigradeChanceCondition))
				}
			}
		)
	}

	private fun createTardigradeChanceCondition(): LootItemCondition {
		val fortune = registries
			.lookupOrThrow(Registries.ENCHANTMENT)
			.getOrThrow(Enchantments.FORTUNE)

		return ToolEnchantmentChanceCondition(
			0.01f,
			LevelBasedValue.perLevel(0.03f, 0.02f),
			fortune
		)
	}

	companion object {
		val TARDIGRADE_FROM_MOSS: ResourceKey<LootTable> = ResourceKey.create(
			Registries.LOOT_TABLE,
			Critterworks.modResource("block/tardigrade_from_moss")
		)
	}
}