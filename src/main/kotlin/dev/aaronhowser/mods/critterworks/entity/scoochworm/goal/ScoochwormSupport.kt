package dev.aaronhowser.mods.critterworks.entity.scoochworm.goal

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction

data class ScoochwormSupport(
	val supportPosition: BlockPos,
	val supportDirection: Direction
)