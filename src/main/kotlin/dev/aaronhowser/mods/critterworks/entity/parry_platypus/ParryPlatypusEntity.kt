package dev.aaronhowser.mods.critterworks.entity.parry_platypus

import dev.aaronhowser.mods.critterworks.entity.control.AmphibiousMoveControl
import dev.aaronhowser.mods.critterworks.entity.goal.LowAmphibiousWanderGoal
import dev.aaronhowser.mods.critterworks.registry.ModEntityTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.AgeableMob
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl
import net.minecraft.world.entity.ai.goal.BreedGoal
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal
import net.minecraft.world.entity.ai.goal.TemptGoal
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation
import net.minecraft.world.entity.ai.navigation.PathNavigation
import net.minecraft.world.entity.animal.Animal
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.level.Level
import net.minecraft.world.level.pathfinder.PathType
import software.bernie.geckolib.animatable.GeoEntity
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.animation.AnimatableManager
import software.bernie.geckolib.animation.AnimationController
import software.bernie.geckolib.animation.AnimationState
import software.bernie.geckolib.animation.PlayState
import software.bernie.geckolib.animation.RawAnimation
import software.bernie.geckolib.util.GeckoLibUtil

class ParryPlatypusEntity(
	entityType: EntityType<out ParryPlatypusEntity>,
	level: Level
) : Animal(entityType, level), GeoEntity {

	private val animatableInstanceCache = GeckoLibUtil.createInstanceCache(this)

	init {
		moveControl = AmphibiousMoveControl(this)
		lookControl = SmoothSwimmingLookControl(this, 10)
		setPathfindingMalus(PathType.WATER, 0f)
	}

	override fun registerGoals() {
		goalSelector.addGoal(1, BreedGoal(this, 1.0))
		goalSelector.addGoal(2, TemptGoal(this, 1.1, FOOD, false))
		goalSelector.addGoal(3, LowAmphibiousWanderGoal(this, 3))
		goalSelector.addGoal(5, LookAtPlayerGoal(this, Player::class.java, 6f))
		goalSelector.addGoal(6, RandomLookAroundGoal(this))
	}

	override fun createNavigation(level: Level): PathNavigation {
		return AmphibiousPathNavigation(this, level)
	}

	override fun isFood(stack: ItemStack): Boolean {
		return FOOD.test(stack)
	}

	override fun getBreedOffspring(level: ServerLevel, otherParent: AgeableMob): AgeableMob? {
		return ModEntityTypes.PARRY_PLATYPUS.get().create(level)
	}

	override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar) {
		controllers.add(AnimationController(this, "movement", 3, ::selectMovementAnimation))
	}

	private fun selectMovementAnimation(animationState: AnimationState<ParryPlatypusEntity>): PlayState {
		if (!animationState.isMoving) return PlayState.STOP

		val animation = if (isInWater) SWIM_ANIMATION else WALK_ANIMATION
		return animationState.setAndContinue(animation)
	}

	override fun getAnimatableInstanceCache(): AnimatableInstanceCache = animatableInstanceCache

	companion object {
		const val WIDTH = 0.875f
		const val HEIGHT = 0.375f

		private val FOOD = Ingredient.of(Items.COD)

		private val WALK_ANIMATION: RawAnimation =
			RawAnimation.begin()
				.thenLoop("animation.platypus.walk")

		private val SWIM_ANIMATION: RawAnimation =
			RawAnimation.begin()
				.thenLoop("animation.platypus.swim")

		fun createAttributes(): AttributeSupplier {
			return createMobAttributes()
				.add(Attributes.MAX_HEALTH, 10.0)
				.add(Attributes.MOVEMENT_SPEED, 0.25)
				.build()
		}
	}
}