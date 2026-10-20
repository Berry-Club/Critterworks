package dev.aaronhowser.mods.critterworks.entity.punchprawn

import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isFluid
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isItem
import dev.aaronhowser.mods.critterworks.entity.punchprawn.goal.PunchprawnWanderGoal
import dev.aaronhowser.mods.critterworks.registry.ModItems
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.tags.FluidTags
import net.minecraft.util.RandomSource
import net.minecraft.world.entity.AgeableMob
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MobSpawnType
import net.minecraft.world.entity.MoverType
import net.minecraft.world.entity.TamableAnimal
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation
import net.minecraft.world.entity.ai.navigation.PathNavigation
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.ServerLevelAccessor
import net.minecraft.world.level.pathfinder.PathType
import net.minecraft.world.phys.Vec3
import net.neoforged.neoforge.fluids.FluidType
import software.bernie.geckolib.animatable.GeoEntity
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.animatable.instance.SingletonAnimatableInstanceCache
import software.bernie.geckolib.animation.AnimatableManager

class PunchprawnEntity(
	entityType: EntityType<out PunchprawnEntity>,
	level: Level
) : TamableAnimal(entityType, level), GeoEntity {

	private val animatableInstanceCache = SingletonAnimatableInstanceCache(this)

	init {
		moveControl = SmoothSwimmingMoveControl(this, 85, 10, 0.02f, 0.03f, true)
		lookControl = SmoothSwimmingLookControl(this, 10)
		setPathfindingMalus(PathType.WATER, 0f)
	}

	override fun registerGoals() {
		goalSelector.addGoal(1, MeleeAttackGoal(this, 1.2, true))
		goalSelector.addGoal(4, PunchprawnWanderGoal(this))

		targetSelector.addGoal(1, HurtByTargetGoal(this))
		targetSelector.addGoal(2, NearestAttackableTargetGoal(this, Player::class.java, true))
	}

	override fun createNavigation(level: Level): PathNavigation {
		return AmphibiousPathNavigation(this, level)
	}

	override fun travel(travelVector: Vec3) {
		if (!isInWater) {
			super.travel(travelVector)
			return
		}

		moveRelative(speed, travelVector)
		move(MoverType.SELF, deltaMovement)
		deltaMovement = deltaMovement.scale(0.9)

		if (target == null) {
			deltaMovement = deltaMovement.add(0.0, -0.005, 0.0)
		}
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

	override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar) {}
	override fun getAnimatableInstanceCache(): AnimatableInstanceCache = animatableInstanceCache

	companion object {
		const val WIDTH = 1f
		const val HEIGHT = 1f

		fun createAttributes(): AttributeSupplier {
			return createMobAttributes()
				.add(Attributes.MOVEMENT_SPEED, 1.2)
				.add(Attributes.ATTACK_DAMAGE, 5.0)
				.add(Attributes.ATTACK_KNOCKBACK, 2.5)
				.build()
		}

		fun checkSpawnRules(
			entityType: EntityType<PunchprawnEntity>,
			level: ServerLevelAccessor,
			spawnType: MobSpawnType,
			position: BlockPos,
			random: RandomSource
		): Boolean {
			return level.getFluidState(position).isFluid(FluidTags.WATER)
		}
	}

}