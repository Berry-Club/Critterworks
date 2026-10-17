package dev.aaronhowser.mods.critterworks.block

import dev.aaronhowser.mods.critterworks.block_entity.ItemFilterfeederBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState

class ItemFilterfeederBlock : Block(
	Properties.ofFullCopy(Blocks.BRAIN_CORAL)
), EntityBlock {

	override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
		return ItemFilterfeederBlockEntity(pos, state)
	}

}