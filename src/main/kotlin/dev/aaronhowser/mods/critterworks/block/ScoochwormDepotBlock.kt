package dev.aaronhowser.mods.critterworks.block

import dev.aaronhowser.mods.critterworks.block.base.ScoochwormTravelBlock
import dev.aaronhowser.mods.critterworks.entity.ScoochwormEntity
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState

class ScoochwormDepotBlock : Block(Properties.ofFullCopy(Blocks.IRON_BLOCK)), ScoochwormTravelBlock {

	override fun canDetachFromBlock(
		blockState: BlockState,
		scoochworm: ScoochwormEntity,
		level: Level,
		position: BlockPos,
		supportDirection: Direction,
		towardsDirection: Direction
	): Boolean {
		return !level.hasNeighborSignal(position)
	}

}