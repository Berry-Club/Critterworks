package dev.aaronhowser.mods.critterworks.entity

import net.minecraft.world.item.ItemStack

interface ItemCatchableEntity {

	fun getAsItemstack(): ItemStack?

}