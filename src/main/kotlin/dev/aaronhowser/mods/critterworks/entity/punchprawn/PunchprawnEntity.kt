package dev.aaronhowser.mods.critterworks.entity.punchprawn

import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isItem
import dev.aaronhowser.mods.critterworks.registry.ModItems
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.AgeableMob
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.TamableAnimal
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.pathfinder.PathType
import net.neoforged.neoforge.fluids.FluidType

class PunchprawnEntity(
	entityType: EntityType<out PunchprawnEntity>,
	level: Level
) : TamableAnimal(entityType, level) {

	init {
		setPathfindingMalus(PathType.WATER, 0f)
	}

	override fun checkSpawnObstruction(level: LevelReader): Boolean {
		return level.isUnobstructed(this)
	}

	override fun isPushedByFluid(type: FluidType): Boolean {
		return false
	}

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