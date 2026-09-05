package dev.aaronhowser.mods.critterworks.block.base

import dev.aaronhowser.mods.critterworks.entity.ScoochwormPartEntity
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.state.BlockState

interface ScoochwormSegmentSupportBlock {

	fun onSegmentAttached(
		state: BlockState,
		level: ServerLevel,
		position: BlockPos,
		segment: ScoochwormPartEntity
	) {
	}

	fun onSegmentTick(
		state: BlockState,
		level: ServerLevel,
		position: BlockPos,
		segment: ScoochwormPartEntity
	) {
	}

	fun onSegmentDetached(
		state: BlockState,
		level: ServerLevel,
		position: BlockPos,
		segment: ScoochwormPartEntity
	) {
	}
}