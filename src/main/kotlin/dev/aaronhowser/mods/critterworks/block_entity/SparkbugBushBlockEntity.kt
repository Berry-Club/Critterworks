package dev.aaronhowser.mods.critterworks.block_entity

import dev.aaronhowser.mods.aaron.block_entity.SyncingBlockEntity
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.toBlockPos
import dev.aaronhowser.mods.critterworks.registry.ModBlockEntityTypes
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.neoforged.neoforge.capabilities.Capabilities

class SparkbugBushBlockEntity(
	pos: BlockPos,
	blockState: BlockState
) : SyncingBlockEntity(ModBlockEntityTypes.SPARKBUG_BUSH.get(), pos, blockState) {

	override val syncImmediately: Boolean = true

	val pollenSpots: List<PollenSpot>
		field = mutableListOf()

	fun addPollenSpot(
		pos: BlockPos,
		direction: Direction,
		isSource: Boolean
	): Boolean {
		val alreadyOne = pollenSpots.any { it.pos == pos && it.direction == direction }
		if (alreadyOne) return false

		val energyHandler = level?.getCapability(Capabilities.EnergyStorage.BLOCK, pos, direction)
		val hasEnergyHandler = energyHandler != null
		if (!hasEnergyHandler) return false

		pollenSpots.add(PollenSpot(pos, direction, isSource))
		setChanged()
		return true
	}

	fun removePollenSpot(
		pos: BlockPos,
		direction: Direction
	): Boolean {
		val success = pollenSpots.removeIf { it.pos == pos && it.direction == direction }
		if (success) setChanged()
		return success
	}

	private fun serverTick(level: ServerLevel) {

	}

	override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
		super.saveAdditional(tag, registries)

		val pollenSpotsTag = ListTag()
		for (spot in pollenSpots) {
			pollenSpotsTag.add(spot.toTag())
		}

		tag.put(POLLEN_SPOTS_TAG, pollenSpotsTag)
	}

	override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
		super.loadAdditional(tag, registries)

		pollenSpots.clear()
		val pollenSpotsTag = tag.getList(POLLEN_SPOTS_TAG, Tag.TAG_COMPOUND.toInt())
		for (i in pollenSpotsTag.indices) {
			val spotTag = pollenSpotsTag.getCompound(i)
			pollenSpots.add(PollenSpot.fromTag(spotTag))
		}
	}

	companion object {
		const val POLLEN_SPOTS_TAG = "pollen_spots"

		fun tick(
			level: Level,
			blockPos: BlockPos,
			blockState: BlockState,
			blockEntity: SparkbugBushBlockEntity
		) {
			if (level is ServerLevel) {
				blockEntity.serverTick(level)
			}
		}
	}

	data class PollenSpot(
		val pos: BlockPos,
		val direction: Direction,
		val isSource: Boolean
	) {

		fun toTag(): CompoundTag {
			val tag = CompoundTag()
			tag.putLong(POS_TAG, pos.asLong())
			tag.putInt(DIRECTION_TAG, direction.ordinal)
			tag.putBoolean(IS_SOURCE_TAG, isSource)
			return tag
		}

		companion object {
			const val POS_TAG = "pos"
			const val DIRECTION_TAG = "direction"
			const val IS_SOURCE_TAG = "is_source"

			fun fromTag(tag: CompoundTag): PollenSpot {
				val pos = tag.getLong(POS_TAG).toBlockPos()
				val directionOrdinal = tag.getInt(DIRECTION_TAG)
				val direction = Direction.entries[directionOrdinal]
				val isSource = tag.getBoolean(IS_SOURCE_TAG)
				return PollenSpot(pos, direction, isSource)
			}
		}
	}

}