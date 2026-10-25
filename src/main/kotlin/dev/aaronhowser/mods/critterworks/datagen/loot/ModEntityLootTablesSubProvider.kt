package dev.aaronhowser.mods.critterworks.datagen.loot

import dev.aaronhowser.mods.aaron.datagen.AaronLootTableDsl
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.cast
import dev.aaronhowser.mods.critterworks.registry.ModEntityTypes
import dev.aaronhowser.mods.critterworks.registry.ModItems
import net.minecraft.core.HolderLookup
import net.minecraft.data.loot.EntityLootSubProvider
import net.minecraft.world.entity.EntityType
import net.minecraft.world.flag.FeatureFlags
import net.minecraft.world.level.storage.loot.predicates.LootItemKilledByPlayerCondition
import java.util.stream.Stream

class ModEntityLootTablesSubProvider(
	registries: HolderLookup.Provider
) : EntityLootSubProvider(FeatureFlags.REGISTRY.allFlags(), registries) {

	override fun generate() {
		add(
			ModEntityTypes.SPROUTSNAIL.get(),
			AaronLootTableDsl.table {
				pool(
					AaronLootTableDsl
						.singleItemChancePool(ModItems.SNAIL_SHELL_FRAGMENT.get(), 0.1f)
						.`when`(LootItemKilledByPlayerCondition.killedByPlayer())
				)
			}
		)
	}

	override fun getKnownEntityTypes(): Stream<EntityType<*>> {
		return listOf(
			ModEntityTypes.SPROUTSNAIL.get()
		).stream().cast()
	}

}