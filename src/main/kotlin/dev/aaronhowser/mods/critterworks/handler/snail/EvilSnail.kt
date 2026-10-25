package dev.aaronhowser.mods.critterworks.handler.snail

import net.minecraft.core.BlockPos
import net.minecraft.core.GlobalPos
import net.minecraft.core.registries.Registries
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3

class EvilSnail(
	var pos: Vec3,
	var dimension: ResourceKey<Level>,
	var destination: GlobalPos? = null
) {

	fun toTag(): CompoundTag {
		val tag = CompoundTag()
		tag.putDouble(POSITION_X_TAG, pos.x)
		tag.putDouble(POSITION_Y_TAG, pos.y)
		tag.putDouble(POSITION_Z_TAG, pos.z)
		tag.putString(DIMENSION_TAG, dimension.location().toString())

		val destination = destination
		if (destination != null) {
			val destinationTag = CompoundTag()
			destinationTag.putLong(POSITION_TAG, destination.pos.asLong())
			destinationTag.putString(DIMENSION_TAG, destination.dimension.location().toString())
			tag.put(DESTINATION_TAG, destinationTag)
		}

		return tag
	}

	companion object {
		private const val POSITION_X_TAG = "PositionX"
		private const val POSITION_Y_TAG = "PositionY"
		private const val POSITION_Z_TAG = "PositionZ"
		private const val POSITION_TAG = "Position"
		private const val DIMENSION_TAG = "Dimension"
		private const val DESTINATION_TAG = "Destination"

		fun fromTag(tag: CompoundTag): EvilSnail {
			val pos = Vec3(
				tag.getDouble(POSITION_X_TAG),
				tag.getDouble(POSITION_Y_TAG),
				tag.getDouble(POSITION_Z_TAG)
			)
			val dimension = getDimension(tag)
			val destination = getDestination(tag)

			return EvilSnail(pos, dimension, destination)
		}

		private fun getDestination(tag: CompoundTag): GlobalPos? {
			if (!tag.contains(DESTINATION_TAG)) return null

			val destinationTag = tag.getCompound(DESTINATION_TAG)
			val dimension = getDimension(destinationTag)
			val position = BlockPos.of(destinationTag.getLong(POSITION_TAG))
			return GlobalPos.of(dimension, position)
		}

		private fun getDimension(tag: CompoundTag): ResourceKey<Level> {
			val location = ResourceLocation.parse(tag.getString(DIMENSION_TAG))
			return ResourceKey.create(Registries.DIMENSION, location)
		}
	}

}