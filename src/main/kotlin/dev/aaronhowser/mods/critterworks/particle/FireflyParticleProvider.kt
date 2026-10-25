package dev.aaronhowser.mods.critterworks.particle

import dev.aaronhowser.mods.aaron.misc.AaronExtensions.nextRange
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
			level.random.nextRange(-0.5, 0.5),
			if (level.random.nextBoolean()) yVelocity else -yVelocity,
			level.random.nextRange(-0.5, 0.5)
		)

		val lifetime = level.random.nextRange(
			FireflyParticle.PARTICLE_MIN_LIFETIME,
			FireflyParticle.PARTICLE_MAX_LIFETIME + 1
		)
		particle.setLifetime(lifetime)
		particle.scale(1.5f)
		particle.pickSprite(sprites)
		return particle
	}
}