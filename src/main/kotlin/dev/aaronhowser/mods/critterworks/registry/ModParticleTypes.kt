package dev.aaronhowser.mods.critterworks.registry

import dev.aaronhowser.mods.critterworks.Critterworks
import net.minecraft.core.particles.ParticleType
import net.minecraft.core.particles.SimpleParticleType
import net.minecraft.core.registries.BuiltInRegistries
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister
import java.util.function.Supplier

object ModParticleTypes {

	val PARTICLE_TYPE_REGISTRY: DeferredRegister<ParticleType<*>> =
		DeferredRegister.create(BuiltInRegistries.PARTICLE_TYPE, Critterworks.MOD_ID)

	val FIREFLY: DeferredHolder<ParticleType<*>, SimpleParticleType> =
		PARTICLE_TYPE_REGISTRY.register("firefly", Supplier { SimpleParticleType(false) })

}