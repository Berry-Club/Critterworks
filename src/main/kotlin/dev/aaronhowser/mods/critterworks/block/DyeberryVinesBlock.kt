package dev.aaronhowser.mods.critterworks.block

import dev.aaronhowser.mods.aaron.misc.AaronExtensions.roll
import dev.aaronhowser.mods.critterworks.entity.scoochworm.data.WormColor
import dev.aaronhowser.mods.critterworks.registry.ModBlocks
import dev.aaronhowser.mods.critterworks.world.DyeberryVineReplacement
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.util.Mth
import net.minecraft.util.RandomSource
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.CaveVines
import net.minecraft.world.level.block.CaveVinesBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.EnumProperty
import net.minecraft.world.level.gameevent.GameEvent
import net.minecraft.world.phys.BlockHitResult

class DyeberryVinesBlock : CaveVinesBlock(Properties.ofFullCopy(Blocks.CAVE_VINES)) {

	init {
		registerDefaultState(
			defaultBlockState()
				.setValue(COLOR, WormColor.GREEN)
		)
	}

	override fun getBodyBlock(): Block = ModBlocks.DYEBERRY_VINES_PLANT.get()

	override fun updateBodyAfterConvertedFromHead(head: BlockState, body: BlockState): BlockState {
		return super.updateBodyAfterConvertedFromHead(head, body)
			.setValue(COLOR, head.getValue(COLOR))
	}

	override fun canSurvive(state: BlockState, level: LevelReader, position: BlockPos): Boolean {
		val supportPosition = position.above()
		val supportState = level.getBlockState(supportPosition)

		return supportState.block is CaveVines
			|| supportState.isFaceSturdy(level, supportPosition, Direction.DOWN)
	}

	override fun updateShape(
		state: BlockState,
		direction: Direction,
		neighborState: BlockState,
		level: LevelAccessor,
		position: BlockPos,
		neighborPosition: BlockPos
	): BlockState {
		if (direction == Direction.DOWN && neighborState.block is CaveVines) {
			return updateBodyAfterConvertedFromHead(state, bodyBlock.defaultBlockState())
		}

		return super.updateShape(state, direction, neighborState, level, position, neighborPosition)
	}

	override fun getGrowIntoState(state: BlockState, random: RandomSource): BlockState {
		val vanillaState = Blocks.CAVE_VINES
			.defaultBlockState()
			.setValue(AGE, state.getValue(AGE) + 1)
			.setValue(BERRIES, random.roll(BERRY_GROWTH_CHANCE))

		return DyeberryVineReplacement.replace(vanillaState, random)
	}

	override fun getCloneItemStack(
		level: LevelReader,
		position: BlockPos,
		state: BlockState
	): ItemStack {
		return state.getValue(COLOR).getDyeberryStack()
	}

	override fun useWithoutItem(
		state: BlockState,
		level: Level,
		position: BlockPos,
		player: Player,
		hitResult: BlockHitResult
	): InteractionResult {
		return harvest(player, state, level, position)
	}

	override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
		super.createBlockStateDefinition(builder)
		builder.add(COLOR)
	}

	companion object {
		private const val BERRY_GROWTH_CHANCE = 0.11f

		val COLOR: EnumProperty<WormColor> = EnumProperty.create("color", WormColor::class.java)

		fun harvest(
			entity: Entity?,
			state: BlockState,
			level: Level,
			position: BlockPos
		): InteractionResult {
			if (!state.getValue(BERRIES)) return InteractionResult.PASS

			val color = state.getValue(COLOR)
			popResource(level, position, color.getDyeberryStack())

			val pitch = Mth.randomBetween(level.random, 0.8f, 1.2f)
			level.playSound(
				null,
				position,
				SoundEvents.CAVE_VINES_PICK_BERRIES,
				SoundSource.BLOCKS,
				1f,
				pitch
			)

			val harvestedState = state.setValue(BERRIES, false)
			level.setBlock(position, harvestedState, UPDATE_CLIENTS)
			level.gameEvent(
				GameEvent.BLOCK_CHANGE,
				position,
				GameEvent.Context.of(entity, harvestedState)
			)

			return InteractionResult.sidedSuccess(level.isClientSide)
		}
	}
}