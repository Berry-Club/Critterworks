package dev.aaronhowser.mods.critterworks.entity.scoochworm.goal

import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isItem
import dev.aaronhowser.mods.critterworks.datagen.tag.ModItemTagsProvider
import dev.aaronhowser.mods.critterworks.entity.scoochworm.ScoochwormEntity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.goal.Goal
import java.util.*

class ScoochwormLookAtFoodGoal(
	private val scoochworm: ScoochwormEntity
) : Goal() {

	private var foodHolder: LivingEntity? = null

	init {
		flags = EnumSet.of(Flag.LOOK)
	}

	override fun canUse(): Boolean {
		foodHolder = findNearestMelonHolder()
		return foodHolder != null
	}

	override fun canContinueToUse(): Boolean {
		val foodHolder = this.foodHolder ?: return false

		return foodHolder.isAlive
			&& isHoldingFood(foodHolder)
			&& scoochworm.distanceToSqr(foodHolder) <= LOOK_DISTANCE_SQUARED
	}

	override fun tick() {
		val player = this.foodHolder ?: return
		scoochworm.lookControl.setLookAt(player, 10f, 40f)
	}

	override fun stop() {
		foodHolder = null
	}

	private fun findNearestMelonHolder(): LivingEntity? {
		val nearbyEntities = scoochworm.level().getEntitiesOfClass(
			LivingEntity::class.java,
			scoochworm.boundingBox.inflate(LOOK_DISTANCE)
		)

		var nearestFoodHolder: LivingEntity? = null
		var nearestDistanceSquared = LOOK_DISTANCE_SQUARED

		for (entity in nearbyEntities) {
			if (entity.isSpectator || !entity.isAlive || !isHoldingFood(entity)) continue

			val distanceSquared = scoochworm.distanceToSqr(entity)
			if (distanceSquared > nearestDistanceSquared) continue

			nearestFoodHolder = entity
			nearestDistanceSquared = distanceSquared
		}

		return nearestFoodHolder
	}

	private fun isHoldingFood(entity: LivingEntity): Boolean {
		return entity.isHolding { it.isItem(ModItemTagsProvider.SCOOCHWORM_LOOK_AT) }
	}

	companion object {
		private const val LOOK_DISTANCE = 6.0
		private const val LOOK_DISTANCE_SQUARED = LOOK_DISTANCE * LOOK_DISTANCE
	}

}