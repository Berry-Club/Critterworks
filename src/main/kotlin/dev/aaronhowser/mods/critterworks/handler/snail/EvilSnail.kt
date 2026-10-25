package dev.aaronhowser.mods.critterworks.handler.snail

import dev.aaronhowser.mods.aaron.misc.AaronExtensions.toComponent
import dev.aaronhowser.mods.critterworks.config.ServerConfig
import dev.aaronhowser.mods.critterworks.datagen.ModDamageTypes
import dev.aaronhowser.mods.critterworks.datagen.language.ModEntityLang
import net.minecraft.core.registries.Registries
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3

class EvilSnail(
	var pos: Vec3,
	var dimension: ResourceKey<Level>,
	var stationaryTicks: Int = STATIONARY_DURATION_TICKS
) {

	fun tick(player: ServerPlayer?): TickResult {
		if (stationaryTicks > 0) {
			stationaryTicks--
			return TickResult.CHANGED
		}

		if (player == null) return TickResult.NO_CHANGE
		if (dimension != player.level().dimension()) return TickResult.NO_CHANGE

		val target = player.position().lerp(player.eyePosition, 0.5)
		val offset = pos.vectorTo(target)
		val distance = offset.length()
		val speed = ServerConfig.CONFIG.sproutsnailEvilSpeed.get()

		if (distance <= speed * 1.25) {
			val damageSource = player.damageSources()
				.source(ModDamageTypes.EVIL_SNAIL)

			player.hurt(damageSource, Float.MAX_VALUE)

			return if (player.isDeadOrDying) {
				TickResult.REMOVE
			} else {
				TickResult.NO_CHANGE
			}
		}

		val movement = offset.scale(minOf(speed, distance) / distance)
		pos = pos.add(movement)
		return TickResult.CHANGED
	}

	fun toTag(): CompoundTag {
		val tag = CompoundTag()
		tag.putDouble(POSITION_X_TAG, pos.x)
		tag.putDouble(POSITION_Y_TAG, pos.y)
		tag.putDouble(POSITION_Z_TAG, pos.z)
		tag.putString(DIMENSION_TAG, dimension.location().toString())
		tag.putInt(STATIONARY_TICKS_TAG, stationaryTicks)

		return tag
	}

	companion object {
		private const val POSITION_X_TAG = "PositionX"
		private const val POSITION_Y_TAG = "PositionY"
		private const val POSITION_Z_TAG = "PositionZ"
		private const val DIMENSION_TAG = "Dimension"
		private const val STATIONARY_TICKS_TAG = "StationaryTicks"
		private const val STATIONARY_DURATION_TICKS = 5 * 20

		fun getSnailName(playerName: Component): MutableComponent {
			return ModEntityLang.EVIL_SNAIL.toComponent(playerName)
		}

		fun fromTag(tag: CompoundTag): EvilSnail {
			val pos = Vec3(
				tag.getDouble(POSITION_X_TAG),
				tag.getDouble(POSITION_Y_TAG),
				tag.getDouble(POSITION_Z_TAG)
			)
			val dimension = getDimension(tag)
			val stationaryTicks = tag.getInt(STATIONARY_TICKS_TAG)

			return EvilSnail(pos, dimension, stationaryTicks)
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