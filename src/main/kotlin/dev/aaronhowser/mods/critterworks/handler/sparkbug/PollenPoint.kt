package dev.aaronhowser.mods.critterworks.handler.sparkbug

import dev.aaronhowser.mods.aaron.misc.AaronExtensions.toBlockPos
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.Level
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.energy.IEnergyStorage

data class PollenPoint(
	val pos: BlockPos,
	val direction: Direction
) {

	fun toDisplaySpot(isInput: Boolean, isActive: Boolean): DisplayPollenPoint {
		return DisplayPollenPoint(pos, direction, isInput, isActive)
	}

	fun getEnergyHandler(level: Level): IEnergyStorage? {
		return level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, direction)
	}

	fun toTag(): CompoundTag {
		val tag = CompoundTag()
		tag.putLong(POS_TAG, pos.asLong())
		tag.putInt(DIRECTION_TAG, direction.ordinal)
		return tag
	}

	companion object {
		private const val POS_TAG = "pos"
		private const val DIRECTION_TAG = "direction"

		fun fromTag(tag: CompoundTag): PollenPoint {
			val pos = tag.getLong(POS_TAG).toBlockPos()
			val directionOrdinal = tag.getInt(DIRECTION_TAG)
			val direction = Direction.entries[directionOrdinal]
			return PollenPoint(pos, direction)
		}
	}

}