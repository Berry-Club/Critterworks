package dev.aaronhowser.mods.critterworks.block

import dev.aaronhowser.mods.critterworks.entity.scoochworm.data.WormColor
import dev.aaronhowser.mods.critterworks.registry.ModBlocks
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.phys.BlockHitResult

class DyeberryVinesPlantBlock : CaveVinesPlantBlock(Properties.ofFullCopy(Blocks.CAVE_VINES_PLANT)) {

	init {
		registerDefaultState(
			defaultBlockState()
				.setValue(DyeberryVinesBlock.COLOR, WormColor.GREEN)
		)
	}

	override fun getHeadBlock(): GrowingPlantHeadBlock = ModBlocks.DYEBERRY_VINES.get()

	override fun updateHeadAfterConvertedFromBody(head: BlockState, body: BlockState): BlockState {
		return super.updateHeadAfterConvertedFromBody(head, body)
			.setValue(DyeberryVinesBlock.COLOR, head.getValue(DyeberryVinesBlock.COLOR))
	}

	override fun canSurvive(state: BlockState, level: LevelReader, position: BlockPos): Boolean {
		val supportPosition = position.above()
		val supportState = level.getBlockState(supportPosition)

		return supportState.block is CaveVines
			|| supportState.isFaceSturdy(level, supportPosition, Direction.DOWN)
	}

	override fun updateShape(
		state: BlockState,
		direction: Direction,
		neighborState: BlockState,
		level: LevelAccessor,
		position: BlockPos,
		neighborPosition: BlockPos
	): BlockState {
		if (direction == Direction.DOWN && neighborState.block is CaveVines) return state

		return super.updateShape(state, direction, neighborState, level, position, neighborPosition)
	}

	override fun getCloneItemStack(
		level: LevelReader,
		position: BlockPos,
		state: BlockState
	): ItemStack {
		return state.getValue(DyeberryVinesBlock.COLOR).getDyeberryStack()
	}

	override fun useWithoutItem(
		state: BlockState,
		level: Level,
		position: BlockPos,
		player: Player,
		hitResult: BlockHitResult
	): InteractionResult {
		return DyeberryVinesBlock.harvest(player, state, level, position)
	}

	override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
		super.createBlockStateDefinition(builder)
		builder.add(DyeberryVinesBlock.COLOR)
	}
}