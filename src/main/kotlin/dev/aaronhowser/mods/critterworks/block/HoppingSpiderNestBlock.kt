package dev.aaronhowser.mods.critterworks.block

import com.mojang.serialization.MapCodec
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isBlock
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isItem
import dev.aaronhowser.mods.critterworks.block_entity.HoppingSpiderNestBlockEntity
import dev.aaronhowser.mods.critterworks.item.HoppingSpiderItem
import dev.aaronhowser.mods.critterworks.registry.ModBlockEntityTypes
import dev.aaronhowser.mods.critterworks.registry.ModItems
import net.minecraft.core.BlockPos
import net.minecraft.world.Containers
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.ItemInteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult

class HoppingSpiderNestBlock(
	properties: Properties = Properties.ofFullCopy(Blocks.OAK_PLANKS)
) : BaseEntityBlock(properties) {

	override fun codec(): MapCodec<out BaseEntityBlock> = CODEC

	override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL

	override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
		return HoppingSpiderNestBlockEntity(pos, state)
	}

	override fun useWithoutItem(
		state: BlockState,
		level: Level,
		pos: BlockPos,
		player: Player,
		hitResult: BlockHitResult
	): InteractionResult {
		val nest = level.getBlockEntity(pos) as? HoppingSpiderNestBlockEntity
			?: return InteractionResult.PASS

		if (player.isShiftKeyDown) {
			if (!level.isClientSide) {
				removeLastSpider(nest, player)
			}

			return InteractionResult.sidedSuccess(level.isClientSide)
		}

		if (!level.isClientSide) {
			player.openMenu(nest) { data -> data.writeBlockPos(nest.blockPos) }
		}

		return InteractionResult.sidedSuccess(level.isClientSide)
	}

	override fun useItemOn(
		stack: ItemStack,
		state: BlockState,
		level: Level,
		position: BlockPos,
		player: Player,
		hand: InteractionHand,
		hitResult: BlockHitResult
	): ItemInteractionResult {
		if (player.isShiftKeyDown || !stack.isItem(ModItems.HOPPING_SPIDER)) {
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
		}

		val nest = level.getBlockEntity(position) as? HoppingSpiderNestBlockEntity
			?: return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION

		if (nest.hoppingSpiders.size >= HoppingSpiderNestBlockEntity.MAX_SPIDERS) {
			return ItemInteractionResult.FAIL
		}

		if (!level.isClientSide && nest.addSpider(stack)) {
			stack.consume(1, player)
		}

		return ItemInteractionResult.sidedSuccess(level.isClientSide)
	}

	private fun removeLastSpider(nest: HoppingSpiderNestBlockEntity, player: Player) {
		val spider = nest.removeLastSpider() ?: return
		giveOrDrop(player, HoppingSpiderItem.createStack(spider))

		if (!spider.carriedStack.isEmpty) {
			giveOrDrop(player, spider.carriedStack)
		}
	}

	private fun giveOrDrop(player: Player, stack: ItemStack) {
		if (!player.addItem(stack)) {
			player.drop(stack, false)
		}
	}

	override fun onRemove(
		state: BlockState,
		level: Level,
		pos: BlockPos,
		newState: BlockState,
		movedByPiston: Boolean
	) {
		if (!state.isBlock(newState.block)) {
			dropSpiders(level, pos)
		}

		super.onRemove(state, level, pos, newState, movedByPiston)
	}

	private fun dropSpiders(level: Level, pos: BlockPos) {
		val nest = level.getBlockEntity(pos)
		if (nest !is HoppingSpiderNestBlockEntity) return

		for (spider in nest.hoppingSpiders) {
			dropItem(level, pos, HoppingSpiderItem.createStack(spider))

			if (!spider.carriedStack.isEmpty) {
				dropItem(level, pos, spider.carriedStack)
			}
		}
	}

	private fun dropItem(level: Level, pos: BlockPos, stack: ItemStack) {
		Containers.dropItemStack(
			level,
			pos.x + 0.5,
			pos.y + 0.5,
			pos.z + 0.5,
			stack
		)
	}

	override fun <T : BlockEntity> getTicker(
		level: Level,
		state: BlockState,
		blockEntityType: BlockEntityType<T>
	): BlockEntityTicker<T>? {
		if (level.isClientSide) return null

		return createTickerHelper(
			blockEntityType,
			ModBlockEntityTypes.HOPPING_SPIDER_NEST.get(),
			HoppingSpiderNestBlockEntity::serverTick
		)
	}

	companion object {
		val CODEC: MapCodec<HoppingSpiderNestBlock> = simpleCodec(::HoppingSpiderNestBlock)
	}
}