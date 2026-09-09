package dev.aaronhowser.mods.critterworks.block

import dev.aaronhowser.mods.critterworks.block.base.ScoochwormTravelBlock
import dev.aaronhowser.mods.critterworks.entity.ScoochwormEntity
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.RotatedPillarBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.BooleanProperty

class ScoochwormDepotBlock : RotatedPillarBlock(Properties.ofFullCopy(Blocks.OAK_LOG)), ScoochwormTravelBlock {

	init {
		registerDefaultState(defaultBlockState().setValue(POWERED, false))
	}

	override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
		super.createBlockStateDefinition(builder)
		builder.add(POWERED)
	}

	override fun getStateForPlacement(context: BlockPlaceContext): BlockState {
		return defaultBlockState()
			.setValue(AXIS, context.clickedFace.axis)
			.setValue(POWERED, context.level.hasNeighborSignal(context.clickedPos))
	}

	override fun neighborChanged(
		state: BlockState,
		level: Level,
		position: BlockPos,
		neighborBlock: Block,
		neighborPosition: BlockPos,
		movedByPiston: Boolean
	) {
		val isPowered = level.hasNeighborSignal(position)
		val wasPowered = state.getValue(POWERED)
		if (wasPowered == isPowered) return

		level.setBlock(position, state.setValue(POWERED, isPowered), UPDATE_CLIENTS)
	}

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

	companion object {
		val POWERED: BooleanProperty = BlockStateProperties.POWERED
	}

}