package dev.aaronhowser.mods.critterworks.block_entity

import dev.aaronhowser.mods.critterworks.registry.ModBlockEntityTypes
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.energy.IEnergyStorage

//TODO cache the IEnergyStorages too instead of recalculating every time?
// but what if between calls the capability changes or something
class SparkbugBushBlock(
	pos: BlockPos,
	blockState: BlockState
) : BlockEntity(ModBlockEntityTypes.SPARKBUG_BUSH.get(), pos, blockState) {

	private val energyStorage = DistributedEnergyStorage()

	private val cachedReceivers: MutableList<BlockEntity> = mutableListOf()
	private fun getCachedEnergyHandlers(): List<IEnergyStorage> {
		val level = level as? ServerLevel ?: return emptyList()

		return cachedReceivers
			.asSequence()
			.filterNot(BlockEntity::isRemoved)
			.mapNotNull {
				DIRECTIONS_OR_NULL.firstNotNullOfOrNull { dir ->
					level.getCapability(Capabilities.EnergyStorage.BLOCK, it.blockPos, dir)
				}
			}
			.toList()
	}

	companion object {
		val DIRECTIONS_OR_NULL = Direction.entries + null
	}

	inner class DistributedEnergyStorage : IEnergyStorage {
		override fun receiveEnergy(toReceive: Int, simulate: Boolean): Int {
			TODO("Not yet implemented")
		}

		override fun extractEnergy(toExtract: Int, simulate: Boolean): Int {
			TODO("Not yet implemented")
		}

		override fun getEnergyStored(): Int {
			TODO("Not yet implemented")
		}

		override fun getMaxEnergyStored(): Int {
			TODO("Not yet implemented")
		}

		override fun canExtract(): Boolean {
			TODO("Not yet implemented")
		}

		override fun canReceive(): Boolean {
			TODO("Not yet implemented")
		}

	}

}