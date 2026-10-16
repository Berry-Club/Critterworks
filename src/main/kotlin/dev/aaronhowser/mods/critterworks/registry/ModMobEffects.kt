package dev.aaronhowser.mods.critterworks.registry

import dev.aaronhowser.mods.aaron.registry.AaronMobEffectsRegistry
import dev.aaronhowser.mods.critterworks.Critterworks
import dev.aaronhowser.mods.critterworks.effect.AaronMobEffect
import dev.aaronhowser.mods.critterworks.effect.DyedMobEffect
import dev.aaronhowser.mods.critterworks.entity.scoochworm.data.WormColor
import net.minecraft.core.Holder
import net.minecraft.core.registries.Registries
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.entity.LivingEntity
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister

object ModMobEffects : AaronMobEffectsRegistry() {

	val MOB_EFFECT_REGISTRY: DeferredRegister<MobEffect> =
		DeferredRegister.create(Registries.MOB_EFFECT, Critterworks.MOD_ID)

	override fun getMobEffectRegistry(): DeferredRegister<MobEffect> = MOB_EFFECT_REGISTRY

	val DYED_GREEN: DeferredHolder<MobEffect, DyedMobEffect> =
		registerDyed(WormColor.GREEN)
	val DYED_BLUE: DeferredHolder<MobEffect, DyedMobEffect> =
		registerDyed(WormColor.BLUE)
	val DYED_RED: DeferredHolder<MobEffect, DyedMobEffect> =
		registerDyed(WormColor.RED)
	val DYED_YELLOW: DeferredHolder<MobEffect, DyedMobEffect> =
		registerDyed(WormColor.YELLOW)
	val DYED_MAGENTA: DeferredHolder<MobEffect, DyedMobEffect> =
		registerDyed(WormColor.MAGENTA)
	val DYED_CYAN: DeferredHolder<MobEffect, DyedMobEffect> =
		registerDyed(WormColor.CYAN)
	val AARON: DeferredHolder<MobEffect, AaronMobEffect> =
		register("aaron", ::AaronMobEffect)

	private fun registerDyed(
		wormColor: WormColor
	): DeferredHolder<MobEffect, DyedMobEffect> {
		return register("dyed_${wormColor.colorName}") {
			DyedMobEffect(wormColor)
		}
	}

	fun getDyedEffect(wormColor: WormColor): Holder<MobEffect> {
		return when (wormColor) {
			WormColor.GREEN -> DYED_GREEN
			WormColor.BLUE -> DYED_BLUE
			WormColor.RED -> DYED_RED
			WormColor.YELLOW -> DYED_YELLOW
			WormColor.MAGENTA -> DYED_MAGENTA
			WormColor.CYAN -> DYED_CYAN
			WormColor.AARON -> AARON
		}
	}

	fun getDyeColor(entity: LivingEntity): WormColor? {
		for (effectInstance in entity.activeEffects) {
			val effect = effectInstance.effect.value()

			if (effect is DyedMobEffect) {
				return effect.wormColor
			}
		}

		return null
	}
}