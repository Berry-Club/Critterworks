package dev.aaronhowser.mods.critterworks.datagen

import dev.aaronhowser.mods.critterworks.Critterworks
import net.minecraft.core.registries.Registries
import net.minecraft.data.worldgen.BootstrapContext
import net.minecraft.resources.ResourceKey
import net.minecraft.world.damagesource.DamageType

object ModDamageTypes {

	val EVIL_SNAIL: ResourceKey<DamageType> = ResourceKey.create(
		Registries.DAMAGE_TYPE,
		Critterworks.modResource("evil_snail")
	)
	val PARRY_PLATYPUS: ResourceKey<DamageType> = ResourceKey.create(
		Registries.DAMAGE_TYPE,
		Critterworks.modResource("parry_platypus")
	)

	fun bootstrap(context: BootstrapContext<DamageType>) {
		context.register(
			EVIL_SNAIL,
			DamageType("evilSnail", 0f)
		)

		context.register(
			PARRY_PLATYPUS,
			DamageType("parryPlatypus", 0.1f)
		)
	}

}