package dev.aaronhowser.mods.critterworks.entity.sproutsnail

import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isItem
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.AgeableMob
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.ai.goal.RandomStrollGoal
import net.minecraft.world.entity.animal.Animal
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BonemealableBlock
import net.minecraft.world.level.block.LevelEvent
import net.neoforged.neoforge.common.Tags
import software.bernie.geckolib.animatable.GeoEntity
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.animation.AnimatableManager
import software.bernie.geckolib.util.GeckoLibUtil

class SproutsnailEntity(
	entityType: EntityType<out SproutsnailEntity>,
	level: Level
) : Animal(entityType, level), GeoEntity {

	private val animatableInstanceCache = GeckoLibUtil.createInstanceCache(this)

	override fun registerGoals() {
		goalSelector.addGoal(0, RandomStrollGoal(this, 1.0))
	}

	override fun isFood(stack: ItemStack): Boolean {
		return stack.isItem(Tags.Items.MUSHROOMS)
	}

	override fun tick() {
		val previousBlockPositions = BlockPos.betweenClosedStream(boundingBox)
			.toList().toSet()

		super.tick()

		val newBlockPositions = BlockPos.betweenClosedStream(boundingBox)
			.toList()
			.toMutableList()

		newBlockPositions.removeAll(previousBlockPositions)

		for (newBlockPosition in newBlockPositions) {
			tryApplyBonemeal(newBlockPosition)
		}
	}

	private fun tryApplyBonemeal(position: BlockPos) {
		val level = level() as? ServerLevel ?: return
		val state = level.getBlockState(position)
		val block = state.block
		if (block !is BonemealableBlock) return

		if (block.isValidBonemealTarget(level, position, state)) {
			block.performBonemeal(level, random, position, state)
			level.levelEvent(LevelEvent.PARTICLES_AND_SOUND_PLANT_GROWTH, position, 0)
		}
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