package dev.aaronhowser.mods.critterworks.entity.sproutsnail

import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isItem
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.AgeableMob
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.ai.goal.RandomStrollGoal
import net.minecraft.world.entity.animal.Animal
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.neoforged.neoforge.common.Tags
import software.bernie.geckolib.animatable.GeoEntity
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.animatable.instance.SingletonAnimatableInstanceCache
import software.bernie.geckolib.animation.AnimatableManager

class SproutsnailEntity(
	entityType: EntityType<out SproutsnailEntity>,
	level: Level
) : Animal(entityType, level), GeoEntity {

	private val animatableInstanceCache = SingletonAnimatableInstanceCache(this)

	override fun registerGoals() {
		goalSelector.addGoal(0, RandomStrollGoal(this, 1.0))
	}

	override fun isFood(stack: ItemStack): Boolean {
		return stack.isItem(Tags.Items.MUSHROOMS)
	}

	override fun getBreedOffspring(level: ServerLevel, otherParent: AgeableMob): AgeableMob? {
		return null
	}

	override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar) {}
	override fun getAnimatableInstanceCache(): AnimatableInstanceCache = animatableInstanceCache

	companion object {
		const val WIDTH = 0.6f
		const val HEIGHT = 0.7f

		fun createAttributes(): AttributeSupplier {
			return createMobAttributes()
				.add(Attributes.MAX_HEALTH, 10.0)
				.add(Attributes.MOVEMENT_SPEED, 0.15)
				.build()
		}
	}

}