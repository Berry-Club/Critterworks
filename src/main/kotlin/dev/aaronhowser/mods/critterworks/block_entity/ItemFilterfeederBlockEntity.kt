package dev.aaronhowser.mods.critterworks.block_entity

import dev.aaronhowser.mods.aaron.misc.AaronExtensions.toBlockPos
import dev.aaronhowser.mods.critterworks.ItemFilterFeederCarrier
import dev.aaronhowser.mods.critterworks.registry.ModBlockEntityTypes
import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState

class ItemFilterfeederBlockEntity(
	pos: BlockPos,
	blockState: BlockState
) : BlockEntity(ModBlockEntityTypes.ITEM_FILTER_FEEDER.get(), pos, blockState) {

	override fun onLoad() {
		super.onLoad()
		val level = this.level
		if (level is ItemFilterFeederCarrier) {
			level.getFilterFeederPositions().add(blockPos.asLong())
		}
	}

	override fun setRemoved() {
		super.setRemoved()
		val level = this.level
		if (level is ItemFilterFeederCarrier) {
			level.getFilterFeederPositions().remove(blockPos.asLong())
		}
	}

	override fun clearRemoved() {
		super.clearRemoved()
		val level = this.level
		if (level is ItemFilterFeederCarrier) {
			level.getFilterFeederPositions().add(blockPos.asLong())
		}
	}

	companion object {
		fun ItemFilterFeederCarrier.getFilterFeederPositions(): LongOpenHashSet = `critterworks$getItemFilterFeederPositions`()

		@JvmStatic
		fun isWithinRange(level: Level, position: BlockPos): Boolean {
			if (level !is ItemFilterFeederCarrier) return false

			val radius = 64.0
			val positions = level.getFilterFeederPositions()

			return positions
				.asSequence()
				.map { it.toBlockPos() }
				.any { it.closerThan(position, radius) }
		}
	}

}