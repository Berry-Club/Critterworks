package dev.aaronhowser.mods.critterworks.handler.sparkbug

import dev.aaronhowser.mods.aaron.misc.AaronExtensions.toBlockPos
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag

data class DisplayPollenPoint(
	val pos: BlockPos,
	val direction: Direction,
	val isInput: Boolean,
	val isActive: Boolean
) {

	fun toPollenPoint(): PollenPoint {
		return PollenPoint(pos, direction)
	}

	fun toTag(): CompoundTag {
		val tag = CompoundTag()
		tag.putLong(POS_TAG, pos.asLong())
		tag.putInt(DIRECTION_TAG, direction.ordinal)
		tag.putBoolean(IS_INPUT_TAG, isInput)
		tag.putBoolean(IS_ACTIVE_TAG, isActive)
		return tag
	}

	companion object {
		private const val POS_TAG = "pos"
		private const val DIRECTION_TAG = "direction"
		private const val IS_INPUT_TAG = "is_input"
		private const val IS_ACTIVE_TAG = "is_active"

		fun fromTag(tag: CompoundTag): DisplayPollenPoint {
			val pos = tag.getLong(POS_TAG).toBlockPos()
			val directionOrdinal = tag.getInt(DIRECTION_TAG)
			val direction = Direction.entries[directionOrdinal]
			val isInput = tag.getBoolean(IS_INPUT_TAG)
			val isActive = tag.getBoolean(IS_ACTIVE_TAG)
			return DisplayPollenPoint(pos, direction, isInput, isActive)
		}
	}

}