package dev.aaronhowser.mods.critterworks.entity.filterfeeder

import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isFluid
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.tags.FluidTags
import net.minecraft.util.RandomSource
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MobSpawnType
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.animal.WaterAnimal
import net.minecraft.world.level.Level
import net.minecraft.world.level.ServerLevelAccessor
import software.bernie.geckolib.animatable.GeoEntity
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.animatable.instance.SingletonAnimatableInstanceCache
import software.bernie.geckolib.animation.AnimatableManager

class FilterfeederEntity(
	entityType: EntityType<out FilterfeederEntity>,
	level: Level
) : WaterAnimal(entityType, level), GeoEntity {

	private val animatableInstanceCache = SingletonAnimatableInstanceCache(this)

	override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar) = Unit
	override fun getAnimatableInstanceCache(): AnimatableInstanceCache = animatableInstanceCache

	companion object {
		const val WIDTH = 0.5f
		const val HEIGHT = 0.125f

		fun createAttributes(): AttributeSupplier {
			return createMobAttributes()
				.add(Attributes.MAX_HEALTH, 4.0)
				.add(Attributes.MOVEMENT_SPEED, 0.1)
				.build()
		}

		fun checkSpawnRules(
			entityType: EntityType<FilterfeederEntity>,
			level: ServerLevelAccessor,
			spawnType: MobSpawnType,
			position: BlockPos,
			random: RandomSource
		): Boolean {
			val floorPosition = position.below()

			return level.getFluidState(position).isFluid(FluidTags.WATER)
				&& level.getBlockState(floorPosition).isFaceSturdy(level, floorPosition, Direction.UP)
		}
	}
}