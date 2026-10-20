package dev.aaronhowser.mods.critterworks.entity.punchprawn

import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isItem
import dev.aaronhowser.mods.critterworks.registry.ModItems
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.AgeableMob
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.TamableAnimal
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level

class PunchprawnEntity(
	entityType: EntityType<out PunchprawnEntity>,
	level: Level
) : TamableAnimal(entityType, level) {

	override fun isFood(stack: ItemStack): Boolean {
		return stack.isItem(ModItems.PEARL)
	}

	override fun getBreedOffspring(level: ServerLevel, otherParent: AgeableMob): AgeableMob? {
		return null
	}

	companion object {
		const val WIDTH = 1f
		const val HEIGHT = 1f
	}

}