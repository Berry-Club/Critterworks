package dev.aaronhowser.mods.critterworks.item

import dev.aaronhowser.mods.critterworks.registry.ModBlocks
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.BoneMealItem
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.gameevent.GameEvent

class SnailSlimeItem(properties: Properties) : BoneMealItem(properties) {

	override fun useOn(context: UseOnContext): InteractionResult {
		val player = context.player
		if (player == null || !player.isSecondaryUseActive) return super.useOn(context)

		val level = context.level
		val clickedFace = context.clickedFace
		val placementPosition = context.clickedPos.relative(clickedFace)
		val faceTowardsSupport = clickedFace.opposite
		val existingState = level.getBlockState(placementPosition)
		val snailSlimeBlock = ModBlocks.SNAIL_SLIME.get()
		val placementState = snailSlimeBlock.getStateForPlacement(
			existingState,
			level,
			placementPosition,
			faceTowardsSupport
		) ?: return InteractionResult.FAIL

		if (!player.mayUseItemAt(placementPosition, clickedFace, context.itemInHand)) {
			return InteractionResult.FAIL
		}

		if (!level.setBlock(placementPosition, placementState, Block.UPDATE_ALL_IMMEDIATE)) {
			return InteractionResult.FAIL
		}

		val soundType = placementState.getSoundType(level, placementPosition, player)
		level.playSound(
			player,
			placementPosition,
			soundType.placeSound,
			SoundSource.BLOCKS,
			(soundType.volume + 1.0f) / 2.0f,
			soundType.pitch * 0.8f
		)
		level.gameEvent(player, GameEvent.BLOCK_PLACE, placementPosition)

		if (!player.hasInfiniteMaterials()) {
			context.itemInHand.shrink(1)
		}

		return InteractionResult.sidedSuccess(level.isClientSide)
	}

}