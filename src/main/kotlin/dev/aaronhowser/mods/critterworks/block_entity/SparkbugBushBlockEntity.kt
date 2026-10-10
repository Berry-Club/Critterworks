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

	val inputPollenSpots: List<PollenSpot>
		field = mutableListOf()

	val outputPollenSpots: List<PollenSpot>
		field = mutableListOf()

	fun addPollenSpot(
		pos: BlockPos,
		direction: Direction,
		isInput: Boolean
	): Boolean {
		val alreadyOne = (inputPollenSpots + outputPollenSpots).any { it.pos == pos && it.direction == direction }
		if (alreadyOne) return false

		val energyHandler = level?.getCapability(Capabilities.EnergyStorage.BLOCK, pos, direction)
		val hasEnergyHandler = energyHandler != null
		if (!hasEnergyHandler) return false

		if (isInput) {
			inputPollenSpots.add(PollenSpot(pos, direction))
		} else {
			outputPollenSpots.add(PollenSpot(pos, direction))
		}

		setChanged()
		return true
	}

	fun removePollenSpot(
		pos: BlockPos,
		direction: Direction
	): Boolean {
		val success = inputPollenSpots.removeIf { it.pos == pos && it.direction == direction }
			|| outputPollenSpots.removeIf { it.pos == pos && it.direction == direction }

		if (success) setChanged()
		return success
	}

	private fun serverTick(level: ServerLevel) {

	}

	override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
		super.saveAdditional(tag, registries)

		val inputSpotsTag = ListTag()
		for (spot in inputPollenSpots) {
			inputSpotsTag.add(spot.toTag())
		}

		val outputSpotsTag = ListTag()
		for (spot in outputPollenSpots) {
			outputSpotsTag.add(spot.toTag())
		}

		tag.put(INPUT_SPOTS_TAG, inputSpotsTag)
		tag.put(OUTPUT_SPOTS_TAG, outputSpotsTag)
	}

	override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
		super.loadAdditional(tag, registries)

		inputPollenSpots.clear()
		outputPollenSpots.clear()

		val inputSpotsTag = tag.getList(INPUT_SPOTS_TAG, Tag.TAG_COMPOUND.toInt())
		for (i in inputSpotsTag.indices) {
			val spotTag = inputSpotsTag.getCompound(i)
			inputPollenSpots.add(PollenSpot.fromTag(spotTag))
		}

		val outputSpotsTag = tag.getList(OUTPUT_SPOTS_TAG, Tag.TAG_COMPOUND.toInt())
		for (i in outputSpotsTag.indices) {
			val spotTag = outputSpotsTag.getCompound(i)
			outputPollenSpots.add(PollenSpot.fromTag(spotTag))
		}
	}

	companion object {
		const val INPUT_SPOTS_TAG = "input_spots"
		const val OUTPUT_SPOTS_TAG = "output_spots"

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
		val direction: Direction
	) {

		fun toTag(): CompoundTag {
			val tag = CompoundTag()
			tag.putLong(POS_TAG, pos.asLong())
			tag.putInt(DIRECTION_TAG, direction.ordinal)
			return tag
		}

		companion object {
			const val POS_TAG = "pos"
			const val DIRECTION_TAG = "direction"

			fun fromTag(tag: CompoundTag): PollenSpot {
				val pos = tag.getLong(POS_TAG).toBlockPos()
				val directionOrdinal = tag.getInt(DIRECTION_TAG)
				val direction = Direction.entries[directionOrdinal]
				return PollenSpot(pos, direction)
			}
		}
	}

}