package dev.aaronhowser.mods.critterworks.block_entity

import dev.aaronhowser.mods.critterworks.registry.ModBlockEntityTypes
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.neoforged.neoforge.energy.IEnergyStorage

class SparkbugBushBlock(
	pos: BlockPos,
	blockState: BlockState
) : BlockEntity(ModBlockEntityTypes.SPARKBUG_BUSH.get(), pos, blockState) {

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