package dev.aaronhowser.mods.critterworks.block.base

import dev.aaronhowser.mods.critterworks.entity.ScoochwormEntity
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState

interface ScoochwormTravelBlock {
	fun canAttachToBlock(
		blockState: BlockState,
		scoochworm: ScoochwormEntity,
		level: Level,
		position: BlockPos,
		supportDirection: Direction,
		fromDirection: Direction
	): Boolean {
		return true
	}

	fun canDetachFromBlock(
		blockState: BlockState,
		scoochworm: ScoochwormEntity,
		level: Level,
		position: BlockPos,
		supportDirection: Direction,
		towardsDirection: Direction
	): Boolean {
		return true
	}
}