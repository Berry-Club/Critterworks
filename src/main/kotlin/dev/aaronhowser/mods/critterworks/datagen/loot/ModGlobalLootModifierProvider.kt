package dev.aaronhowser.mods.critterworks.datagen.loot

import dev.aaronhowser.mods.critterworks.Critterworks
import net.minecraft.core.HolderLookup
import net.minecraft.data.PackOutput
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition
import net.neoforged.neoforge.common.data.GlobalLootModifierProvider
import net.neoforged.neoforge.common.loot.AddTableLootModifier
import java.util.concurrent.CompletableFuture

class ModGlobalLootModifierProvider(
	output: PackOutput,
	lookupProvider: CompletableFuture<HolderLookup.Provider>
) : GlobalLootModifierProvider(output, lookupProvider, Critterworks.MOD_ID) {

	override fun start() {
		add(
			"tardigrade_from_moss",
			AddTableLootModifier(
				arrayOf(
					LootItemBlockStatePropertyCondition
						.hasBlockStateProperties(Blocks.MOSS_BLOCK)
						.build()
				),
				ModOtherLootTablesSubProvider.TARDIGRADE_FROM_MOSS
			)
		)
	}
}