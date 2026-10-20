package dev.aaronhowser.mods.critterworks.entity.punchprawn

import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isFluid
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isItem
import dev.aaronhowser.mods.critterworks.registry.ModItems
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.tags.FluidTags
import net.minecraft.util.RandomSource
import net.minecraft.world.entity.AgeableMob
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MobSpawnType
import net.minecraft.world.entity.TamableAnimal
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.ServerLevelAccessor
import net.minecraft.world.level.pathfinder.PathType
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

	override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar) {}
	override fun getAnimatableInstanceCache(): AnimatableInstanceCache = animatableInstanceCache

	companion object {
		const val WIDTH = 1f
		const val HEIGHT = 1f

		fun createAttributes(): AttributeSupplier {
			return createMobAttributes()
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