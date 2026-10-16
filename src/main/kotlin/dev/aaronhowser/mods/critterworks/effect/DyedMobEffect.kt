package dev.aaronhowser.mods.critterworks.effect

import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isHolder
import dev.aaronhowser.mods.critterworks.entity.scoochworm.data.WormColor
import dev.aaronhowser.mods.critterworks.registry.ModMobEffectTags
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectCategory
import net.minecraft.world.entity.LivingEntity

class DyedMobEffect(
	val wormColor: WormColor
) : MobEffect(
	MobEffectCategory.NEUTRAL,
	wormColor.tintColor
) {

	override fun onEffectStarted(
		livingEntity: LivingEntity,
		amplifier: Int
	) {
		val activeEffects = livingEntity.activeEffects.toList()

		for (effectInstance in activeEffects) {
			val effect = effectInstance.effect

			if (effect.value() === this) continue
			if (!effect.isHolder(ModMobEffectTags.DYES_ENTITY)) continue

			livingEntity.removeEffect(effect)
		}
	}
}