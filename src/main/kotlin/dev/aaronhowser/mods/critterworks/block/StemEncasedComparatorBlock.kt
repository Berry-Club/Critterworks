package dev.aaronhowser.mods.critterworks.block

import dev.aaronhowser.mods.aaron.container.ContainerContainer
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isBlock
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isItem
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isServerSide
import dev.aaronhowser.mods.critterworks.block.base.ScoochwormSegmentSupportBlock
import dev.aaronhowser.mods.critterworks.entity.ScoochwormEntity
import dev.aaronhowser.mods.critterworks.entity.ScoochwormPartEntity
import dev.aaronhowser.mods.critterworks.block_entity.StemEncasedComparatorBlockEntity
import dev.aaronhowser.mods.critterworks.item.ItemFilterItem
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.RandomSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.ItemInteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.BlockHitResult
import net.neoforged.neoforge.items.ItemHandlerHelper

class StemEncasedComparatorBlock : ScoochstemBlock(), ScoochwormSegmentSupportBlock, EntityBlock {

	override fun useItemOn(
		stack: ItemStack,
		state: BlockState,
		level: Level,
		position: BlockPos,
		player: Player,
		hand: InteractionHand,
		hitResult: BlockHitResult
	): ItemInteractionResult {
		if (stack.isItem(Items.BONE_MEAL)) {
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
		}

		if (level.isServerSide) {
			val blockEntity = level.getBlockEntity(position)
			if (blockEntity is StemEncasedComparatorBlockEntity) {
				player.openMenu(blockEntity)
			}
		}

		return ItemInteractionResult.sidedSuccess(level.isClientSide)
	}

	override fun newBlockEntity(position: BlockPos, state: BlockState): BlockEntity =
		StemEncasedComparatorBlockEntity(position, state)

	override fun isSignalSource(state: BlockState): Boolean = true

	override fun getSignal(
		state: BlockState,
		level: BlockGetter,
		position: BlockPos,
		direction: Direction
	): Int {
		if (level !is Level) return 0

		return calculateOutputSignal(level, position)
	}

	override fun onSegmentTick(
		state: BlockState,
		level: ServerLevel,
		position: BlockPos,
		segment: ScoochwormPartEntity
	) {
		notifyNeighbors(level, position)
	}

	override fun onSegmentDetached(
		state: BlockState,
		level: ServerLevel,
		position: BlockPos,
		segment: ScoochwormPartEntity
	) {
		level.scheduleTick(position, this, 1)
	}

	private fun notifyNeighbors(level: ServerLevel, position: BlockPos) {
		level.updateNeighborsAt(position, this)
	}

	override fun tick(
		state: BlockState,
		level: ServerLevel,
		position: BlockPos,
		random: RandomSource
	) {
		super.tick(state, level, position, random)
		notifyNeighbors(level, position)
	}

	private fun calculateOutputSignal(level: Level, position: BlockPos): Int {
		val blockEntity = level.getBlockEntity(position) as? StemEncasedComparatorBlockEntity
		val filter = blockEntity?.filterContainer?.getItem(0) ?: ItemStack.EMPTY
		if (!filter.isEmpty) {
			return if (hasMatchingWormItem(level, position, filter)) 15 else 0
		}

		val searchBounds = AABB(position).inflate(ScoochwormEntity.SIZE.toDouble())
		val bodyParts = level.getEntitiesOfClass(ScoochwormPartEntity::class.java, searchBounds)
		var strongestSignal = 0

		for (bodyPart in bodyParts) {
			val supportPosition = ScoochwormEntity.getSupportBlockPosition(
				bodyPart.position(),
				bodyPart.supportDirection
			)
			if (supportPosition != position) continue

			val signal = ItemHandlerHelper.calcRedstoneFromInventory(bodyPart.getItemHandler())
			strongestSignal = maxOf(strongestSignal, signal)
		}

		return strongestSignal
	}

	private fun hasMatchingWormItem(level: Level, position: BlockPos, filter: ItemStack): Boolean {
		val searchBounds = AABB(position).inflate(ScoochwormEntity.SIZE.toDouble())
		val bodyParts = level.getEntitiesOfClass(ScoochwormPartEntity::class.java, searchBounds)

		for (bodyPart in bodyParts) {
			val itemHandler = bodyPart.getItemHandler() ?: continue
			val supportPosition = ScoochwormEntity.getSupportBlockPosition(bodyPart.position(), bodyPart.supportDirection)
			if (supportPosition != position) continue
			for (slot in 0 until itemHandler.slots) {
				if (ItemFilterItem.passesFilter(filter, itemHandler.getStackInSlot(slot))) return true
			}
		}

		return false
	}

	override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, movedByPiston: Boolean) {
		if (!state.isBlock(newState.block)) {
			val be = level.getBlockEntity(pos)
			if (be is ContainerContainer) {
				be.dropContents(level, pos)
			}
		}

		super.onRemove(state, level, pos, newState, movedByPiston)
	}

}