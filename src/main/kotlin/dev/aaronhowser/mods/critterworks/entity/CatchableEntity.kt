package dev.aaronhowser.mods.critterworks.entity

import net.minecraft.world.item.ItemStack

interface CatchableEntity {

	fun getCaughtItemStack(): ItemStack

}