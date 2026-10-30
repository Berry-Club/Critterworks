package dev.aaronhowser.mods.critterworks.item.arachnode

import dev.aaronhowser.mods.critterworks.handler.web.redstone.RedstoneWebLine
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3

data class ArachnodePlacementContext(
	val originalLine: RedstoneWebLine,
	val position: Vec3,
	val playerPosition: Vec3,
	val itemStack: ItemStack
)