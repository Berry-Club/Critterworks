package dev.aaronhowser.mods.critterworks.item.arachnode

import dev.aaronhowser.mods.critterworks.handler.web.redstone.RedstoneWebLine
import dev.aaronhowser.mods.critterworks.handler.web.redstone.node.ArachnodeMount
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3

sealed class ArachnodePlacementContext {

	abstract val position: Vec3
	abstract val playerPosition: Vec3
	abstract val itemStack: ItemStack

	class OnLine(
		val originalLine: RedstoneWebLine,
		override val position: Vec3,
		override val playerPosition: Vec3,
		override val itemStack: ItemStack
	) : ArachnodePlacementContext()

	class OnSurface(
		val mount: ArachnodeMount.Surface,
		override val position: Vec3,
		override val playerPosition: Vec3,
		override val itemStack: ItemStack
	) : ArachnodePlacementContext()
}