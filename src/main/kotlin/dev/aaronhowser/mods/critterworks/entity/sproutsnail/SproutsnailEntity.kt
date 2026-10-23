package dev.aaronhowser.mods.critterworks.entity.sproutsnail

import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isItem
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.AgeableMob
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.animal.Animal
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.neoforged.neoforge.common.Tags

class SproutsnailEntity(
	entityType: EntityType<out SproutsnailEntity>,
	level: Level
) : Animal(entityType, level) {

	override fun isFood(stack: ItemStack): Boolean {
		return stack.isItem(Tags.Items.MUSHROOMS)
	}

	override fun getBreedOffspring(level: ServerLevel, otherParent: AgeableMob): AgeableMob? {
		return null
	}

	companion object {
		const val WIDTH = 0.6f
		const val HEIGHT = 0.7f
	}

}