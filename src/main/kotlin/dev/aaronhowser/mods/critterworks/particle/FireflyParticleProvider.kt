package dev.aaronhowser.mods.critterworks.particle

import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.particle.Particle
import net.minecraft.client.particle.ParticleProvider
import net.minecraft.client.particle.SpriteSet
import net.minecraft.core.particles.SimpleParticleType

class FireflyParticleProvider(
	private val sprites: SpriteSet
) : ParticleProvider<SimpleParticleType> {

	override fun createParticle(
		particleType: SimpleParticleType,
		level: ClientLevel,
		x: Double,
		y: Double,
		z: Double,
		xVelocity: Double,
		yVelocity: Double,
		zVelocity: Double
	): Particle {
		val particle = FireflyParticle(
			level,
			x,
			y,
			z,
			0.5 - level.random.nextDouble(),
			if (level.random.nextBoolean()) yVelocity else -yVelocity,
			0.5 - level.random.nextDouble()
		)

		val lifetimeRange =
			FireflyParticle.PARTICLE_MAX_LIFETIME - FireflyParticle.PARTICLE_MIN_LIFETIME + 1
		val lifetime = level.random.nextInt(lifetimeRange) + FireflyParticle.PARTICLE_MIN_LIFETIME
		particle.setLifetime(lifetime)
		particle.scale(1.5f)
		particle.pickSprite(sprites)
		return particle
	}
}