package dev.aaronhowser.mods.critterworks.entity.filterfeeder

import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.animal.WaterAnimal
import net.minecraft.world.level.Level
import software.bernie.geckolib.animatable.GeoEntity
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.animatable.instance.SingletonAnimatableInstanceCache
import software.bernie.geckolib.animation.AnimatableManager

class FilterfeederEntity(
	entityType: EntityType<out FilterfeederEntity>,
	level: Level
) : WaterAnimal(entityType, level), GeoEntity {

	override fun onAddedToLevel() {
		super.onAddedToLevel()
		val player = level().players().firstOrNull()
		val pos = position()
		player?.teleportTo(pos.x, pos.y, pos.z)
	}

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
	}
}