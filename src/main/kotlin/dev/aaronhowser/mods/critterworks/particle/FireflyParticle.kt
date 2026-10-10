package dev.aaronhowser.mods.critterworks.particle

import dev.aaronhowser.mods.aaron.misc.AaronExtensions.nextRange
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.particle.ParticleRenderType
import net.minecraft.client.particle.TextureSheetParticle
import net.minecraft.core.BlockPos
import net.minecraft.util.Mth

class FireflyParticle(
	level: ClientLevel,
	x: Double,
	y: Double,
	z: Double,
	xVelocity: Double,
	yVelocity: Double,
	zVelocity: Double
) : TextureSheetParticle(level, x, y, z, xVelocity, yVelocity, zVelocity) {

	init {
		speedUpWhenYMotionIsBlocked = true
		friction = 0.96f
		quadSize *= 0.75f
		setAlpha(0f)
		yd *= 0.8f
		xd *= 0.8f
		zd *= 0.8f
	}

	override fun getRenderType(): ParticleRenderType {
		return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT
	}

	override fun getLightColor(partialTick: Float): Int {
		val lifetimeProgress = getLifetimeProgress(age + partialTick)
		val fadeAmount = getFadeAmount(
			lifetimeProgress,
			PARTICLE_FADE_IN_LIGHT_TIME,
			PARTICLE_FADE_OUT_LIGHT_TIME
		)
		return (255.0f * fadeAmount).toInt()
	}

	override fun tick() {
		super.tick()

		val currentBlockPos = BlockPos.containing(x, y, z)
		if (!level.getBlockState(currentBlockPos).isAir) {
			remove()
			return
		}

		val lifetimeProgress = getLifetimeProgress(age.toFloat())
		val fadeAmount = getFadeAmount(
			lifetimeProgress,
			PARTICLE_FADE_IN_ALPHA_TIME,
			PARTICLE_FADE_OUT_ALPHA_TIME
		)
		setAlpha(fadeAmount)

		if (Math.random() > 0.95 || age == 1) {
			setParticleSpeed(
				random.nextRange(-0.05, 0.05),
				random.nextRange(-0.05, 0.05),
				random.nextRange(-0.05, 0.05)
			)
		}
	}

	private fun getLifetimeProgress(currentAge: Float): Float {
		return Mth.clamp(currentAge / lifetime, 0f, 1f)
	}

	private fun getFadeAmount(
		lifetimeProgress: Float,
		fadeInTime: Float,
		fadeOutTime: Float
	): Float {
		if (lifetimeProgress >= 1f - fadeInTime) {
			return (1f - lifetimeProgress) / fadeInTime
		}

		if (lifetimeProgress <= fadeOutTime) {
			return lifetimeProgress / fadeOutTime
		}

		return 1f
	}

	companion object {
		private const val PARTICLE_FADE_OUT_LIGHT_TIME = 0.3f
		private const val PARTICLE_FADE_IN_LIGHT_TIME = 0.1f
		private const val PARTICLE_FADE_OUT_ALPHA_TIME = 0.5f
		private const val PARTICLE_FADE_IN_ALPHA_TIME = 0.3f

		const val PARTICLE_MIN_LIFETIME = 36
		const val PARTICLE_MAX_LIFETIME = 180
	}

}