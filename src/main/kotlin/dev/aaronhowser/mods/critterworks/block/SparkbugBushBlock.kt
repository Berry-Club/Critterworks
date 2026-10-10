package dev.aaronhowser.mods.critterworks.block

import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.SoundType
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
}