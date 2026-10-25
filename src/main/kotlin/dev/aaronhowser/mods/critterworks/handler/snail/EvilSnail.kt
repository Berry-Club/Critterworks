package dev.aaronhowser.mods.critterworks.handler.snail

import dev.aaronhowser.mods.critterworks.config.ServerConfig
import dev.aaronhowser.mods.critterworks.datagen.ModDamageTypes
import net.minecraft.core.registries.Registries
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.Level
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3

class EvilSnail(
	var pos: Vec3,
	var dimension: ResourceKey<Level>,
	var accelerationTicksRemaining: Int = ACCELERATION_DURATION_TICKS
) {

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

		val isTouchingPlayer = player.boundingBox.intersects(boundingBox)

		if (isTouchingPlayer) {
			val damageSource = player.damageSources()
				.source(ModDamageTypes.EVIL_SNAIL)

			player.hurt(damageSource, Float.MAX_VALUE)
			return TickResult.REMOVE
		}

		return TickResult.CHANGED
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
		private const val ACCELERATION_DURATION_TICKS = 10 * 20

		fun fromTag(tag: CompoundTag): EvilSnail {
			val pos = Vec3(
				tag.getDouble(POSITION_X_TAG),
				tag.getDouble(POSITION_Y_TAG),
				tag.getDouble(POSITION_Z_TAG)
			)

			val dimension = getDimension(tag)
			val accelerationTicksRemaining = tag.getInt(ACCELERATION_TICKS_TAG)

			return EvilSnail(pos, dimension, accelerationTicksRemaining)
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