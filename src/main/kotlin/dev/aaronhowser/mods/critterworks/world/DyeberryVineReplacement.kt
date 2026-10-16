package dev.aaronhowser.mods.critterworks.world

import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isBlock
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.random
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.roll
import dev.aaronhowser.mods.critterworks.block.DyeberryVinesBlock
import dev.aaronhowser.mods.critterworks.config.ServerConfig
import dev.aaronhowser.mods.critterworks.entity.scoochworm.data.WormColor
import dev.aaronhowser.mods.critterworks.registry.ModBlocks
import net.minecraft.util.RandomSource
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.CaveVines
import net.minecraft.world.level.block.GrowingPlantHeadBlock
import net.minecraft.world.level.block.state.BlockState

object DyeberryVineReplacement {

	private val colors = WormColor.entries.filter { it != WormColor.AARON }

	@JvmStatic
	fun replace(state: BlockState, random: RandomSource): BlockState {
		val isHead = state.isBlock(Blocks.CAVE_VINES)
		val isPlant = state.isBlock(Blocks.CAVE_VINES_PLANT)

		if (!isHead && !isPlant) return state
		if (!state.getValue(CaveVines.BERRIES)) return state
		if (!random.roll(ServerConfig.CONFIG.dyeberryVineReplacementChance.get())) return state

		val color = if (random.roll(AARON_CHANCE)) {
			WormColor.AARON
		} else {
			colors.random(random)
		}
		val replacementBlock = if (isPlant) {
			ModBlocks.DYEBERRY_VINES_PLANT.get()
		} else {
			ModBlocks.DYEBERRY_VINES.get()
		}

		var replacementState = replacementBlock
			.defaultBlockState()
			.setValue(CaveVines.BERRIES, true)
			.setValue(DyeberryVinesBlock.COLOR, color)

		if (isHead) {
			replacementState = replacementState.setValue(
				GrowingPlantHeadBlock.AGE,
				state.getValue(GrowingPlantHeadBlock.AGE)
			)
		}

		return replacementState
	}

	private const val AARON_CHANCE = 0.01f
}