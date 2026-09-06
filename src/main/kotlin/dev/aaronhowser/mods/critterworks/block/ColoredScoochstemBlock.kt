package dev.aaronhowser.mods.critterworks.block

import dev.aaronhowser.mods.critterworks.block.base.ScoochwormTravelBlock
import dev.aaronhowser.mods.critterworks.entity.ScoochwormEntity
import dev.aaronhowser.mods.critterworks.entity.data.WormColor
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.RotatedPillarBlock
import net.minecraft.world.level.block.state.BlockState

class ColoredScoochstemBlock(
	val color: WormColor
) : RotatedPillarBlock(Properties.ofFullCopy(Blocks.OAK_LOG)), ScoochwormTravelBlock {

	override fun canAttachToBlock(
		blockState: BlockState,
		scoochworm: ScoochwormEntity,
		level: Level,
		position: BlockPos,
		supportDirection: Direction,
		fromDirection: Direction
	): Boolean {
		return scoochworm.color == this.color
	}

	override fun getFlammability(
		state: BlockState,
		level: BlockGetter,
		position: BlockPos,
		direction: Direction
	): Int = 5

	override fun getFireSpreadSpeed(
		state: BlockState,
		level: BlockGetter,
		position: BlockPos,
		direction: Direction
	): Int = 5
}