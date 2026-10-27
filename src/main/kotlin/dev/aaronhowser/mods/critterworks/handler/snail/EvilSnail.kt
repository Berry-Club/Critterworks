package dev.aaronhowser.mods.critterworks.handler.snail

import dev.aaronhowser.mods.aaron.misc.AaronExtensions.oneIn
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.randomPos
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.tell
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.toComponent
import dev.aaronhowser.mods.critterworks.config.ServerConfig
import dev.aaronhowser.mods.critterworks.datagen.ModDamageTypes
import dev.aaronhowser.mods.critterworks.datagen.language.ModMessageLang
import dev.aaronhowser.mods.critterworks.item.SnailShieldItem.SnailShieldState
import dev.aaronhowser.mods.critterworks.registry.ModAttachmentTypes
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.core.registries.Registries
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.Mth
import net.minecraft.world.level.Level
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import kotlin.math.cos
import kotlin.math.sin

class EvilSnail(
	pos: Vec3,
	dimension: ResourceKey<Level>,
	accelerationTicksRemaining: Int = ACCELERATION_DURATION_TICKS,
	shieldHits: Int = 0
) {

	var pos = pos
		private set
	var dimension = dimension
		private set
	var accelerationTicksRemaining = accelerationTicksRemaining
		private set
	var shieldHits = shieldHits
		private set

	var boundingBox: AABB = AABB.ofSize(pos, 1.0, 1.0, 1.0)
		private set

	fun tick(player: ServerPlayer?): TickResult {
		if (accelerationTicksRemaining > 0) {
			accelerationTicksRemaining--
		}

		if (player == null) return TickResult.NO_CHANGE
		if (dimension != player.level().dimension()) return TickResult.NO_CHANGE

		val target = player.position().lerp(player.eyePosition, 0.5)
		moveTowards(target)

		if (player.random.oneIn(20)) {
			val particlePos = boundingBox.randomPos(player.random)

			player.serverLevel().sendParticles(
				ParticleTypes.SCULK_SOUL,
				particlePos.x,
				particlePos.y,
				particlePos.z,
				1,
				0.0,
				0.0,
				0.0,
				0.0
			)
		}

		if (player.boundingBox.intersects(boundingBox)) {
			val isShieldActive = player
				.getData(ModAttachmentTypes.SNAIL_SHIELD_STATE) != SnailShieldState.UNSHELLED
			val damageSource = player.damageSources()
				.source(ModDamageTypes.EVIL_SNAIL)

			player.hurt(damageSource, Float.MAX_VALUE)

			if (isShieldActive) {
				return handleShieldCollision(player)
			}

			return TickResult.REMOVE
		}

		return TickResult.CHANGED
	}

	private fun handleShieldCollision(player: ServerPlayer): TickResult {
		shieldHits++
		val requiredShieldHits = ServerConfig.CONFIG.sproutsnailEvilShieldHits.get()

		if (shieldHits >= requiredShieldHits) {
			player.tell(ModMessageLang.EVIL_SNAIL_BANISHED_MESSAGE.toComponent())
			return TickResult.REMOVE
		}

		val restartDistance = ServerConfig.CONFIG.sproutsnailEvilCageRestartDistance.get()
		val angle = player.random.nextDouble() * Mth.TWO_PI.toDouble()
		val restartPosition = player.position()
			.add(
				cos(angle) * restartDistance,
				0.0,
				sin(angle) * restartDistance
			)

		restartAt(restartPosition, player.level().dimension())
		player.tell(
			ModMessageLang.EVIL_SNAIL_SHIELD_MESSAGE
				.toComponent(requiredShieldHits - shieldHits)
		)
		return TickResult.CHANGED
	}

	fun restartAt(position: Vec3, newDimension: ResourceKey<Level>) {
		pos = position
		dimension = newDimension
		accelerationTicksRemaining = ACCELERATION_DURATION_TICKS
		boundingBox = AABB.ofSize(pos, 1.0, 1.0, 1.0)
	}

	private fun moveTowards(target: Vec3) {
		val offset = pos.vectorTo(target)
		val distance = offset.length()

		val normalSpeed = ServerConfig.CONFIG.sproutsnailEvilSpeed.get()
		val elapsedAccelerationTicks = ACCELERATION_DURATION_TICKS - accelerationTicksRemaining
		val accelerationProgress = (elapsedAccelerationTicks.toDouble() / ACCELERATION_DURATION_TICKS)
			.coerceIn(0.0, 1.0)
		val speed = normalSpeed * accelerationProgress * accelerationProgress

		val movement = offset.scale(minOf(speed, distance) / distance)
		pos = pos.add(movement)

		boundingBox = AABB.ofSize(pos, 1.0, 1.0, 1.0)
	}

	fun toTag(): CompoundTag {
		val tag = CompoundTag()
		tag.putDouble(POSITION_X_TAG, pos.x)
		tag.putDouble(POSITION_Y_TAG, pos.y)
		tag.putDouble(POSITION_Z_TAG, pos.z)
		tag.putString(DIMENSION_TAG, dimension.location().toString())
		tag.putInt(SHIELD_HITS_TAG, shieldHits)

		if (accelerationTicksRemaining > 0) {
			tag.putInt(ACCELERATION_TICKS_TAG, accelerationTicksRemaining)
		}

		return tag
	}

	companion object {
		private const val POSITION_X_TAG = "PositionX"
		private const val POSITION_Y_TAG = "PositionY"
		private const val POSITION_Z_TAG = "PositionZ"
		private const val DIMENSION_TAG = "Dimension"
		private const val ACCELERATION_TICKS_TAG = "AccelerationTicks"
		private const val SHIELD_HITS_TAG = "ShieldHits"
		private const val ACCELERATION_DURATION_TICKS = 10 * 20

		fun fromTag(tag: CompoundTag): EvilSnail {
			val pos = Vec3(
				tag.getDouble(POSITION_X_TAG),
				tag.getDouble(POSITION_Y_TAG),
				tag.getDouble(POSITION_Z_TAG)
			)

			val dimension = getDimension(tag)
			val accelerationTicksRemaining = tag.getInt(ACCELERATION_TICKS_TAG)
			val shieldHits = tag.getInt(SHIELD_HITS_TAG)

			return EvilSnail(
				pos,
				dimension,
				accelerationTicksRemaining,
				shieldHits
			)
		}

		private fun getDimension(tag: CompoundTag): ResourceKey<Level> {
			val location = ResourceLocation.parse(tag.getString(DIMENSION_TAG))
			return ResourceKey.create(Registries.DIMENSION, location)
		}
	}

	enum class TickResult {
		NO_CHANGE,
		CHANGED,
		REMOVE
	}

}