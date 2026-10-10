package dev.aaronhowser.mods.critterworks.block

import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isBlock
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.tags.BlockTags
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.material.MapColor
import net.minecraft.world.level.material.PushReaction

class SparkbugBushBlock : Block(
	Properties.of()
		.mapColor(MapColor.PLANT)
		.ignitedByLava()
		.lightLevel { 2 }
		.noCollission()
		.instabreak()
		.sound(SoundType.SWEET_BERRY_BUSH)
		.pushReaction(PushReaction.DESTROY)
) {

	override fun canSurvive(state: BlockState, level: LevelReader, pos: BlockPos): Boolean {
		val posBelow = pos.below()
		val stateBelow = level.getBlockState(posBelow)
		val canBelowSustain = stateBelow.canSustainPlant(level, posBelow, Direction.UP, state)
		if (canBelowSustain.isDefault) return canBelowSustain.isTrue
		return state.isBlock(BlockTags.DIRT)
	}

	override fun updateShape(
		state: BlockState,
		direction: Direction,
		neighborState: BlockState,
		level: LevelAccessor,
		pos: BlockPos,
		neighborPos: BlockPos
	): BlockState {
		return if (state.canSurvive(level, pos)) {
			super.updateShape(state, direction, neighborState, level, pos, neighborPos)
		} else {
			Blocks.AIR.defaultBlockState()
		}
	}

	override fun propagatesSkylightDown(state: BlockState, level: BlockGetter, pos: BlockPos): Boolean {
		return state.fluidState.isEmpty
	}

}