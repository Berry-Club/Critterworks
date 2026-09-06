package dev.aaronhowser.mods.critterworks.block

import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isBlock
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isItem
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.roll
import dev.aaronhowser.mods.critterworks.block.base.ScoochwormTravelBlock
import dev.aaronhowser.mods.critterworks.datagen.tag.ModBlockTagsProvider
import dev.aaronhowser.mods.critterworks.entity.ScoochwormEntity
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundSource
import net.minecraft.util.RandomSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.ItemInteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.RotatedPillarBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.block.state.properties.IntegerProperty
import net.minecraft.world.phys.BlockHitResult
import net.neoforged.neoforge.common.ItemAbilities
import net.neoforged.neoforge.common.ItemAbility

open class ScoochstemBlock : RotatedPillarBlock(Properties.ofFullCopy(Blocks.OAK_LOG)), ScoochwormTravelBlock {

	init {
		registerDefaultState(
			defaultBlockState()
				.setValue(NORTH_DISABLED, false)
				.setValue(EAST_DISABLED, false)
				.setValue(SOUTH_DISABLED, false)
				.setValue(WEST_DISABLED, false)
				.setValue(UP_DISABLED, false)
				.setValue(DOWN_DISABLED, false)
				.setValue(GROWTH_REMAINING, 0)
		)
	}

	private fun grow(
		level: Level,
		fromPosition: BlockPos,
		direction: Direction,
		growthRemaining: Int
	) {
		if (growthRemaining <= 0) return

		val growthPosition = fromPosition.relative(direction)

		val stateThere = level.getBlockState(growthPosition)
		if (!stateThere.canBeReplaced() && !stateThere.isBlock(ModBlockTagsProvider.SCOOCHSTEM_REPLACEABLE)) return

		val growthState = defaultBlockState()
			.setValue(AXIS, direction.axis)
			.setValue(GROWTH_REMAINING, growthRemaining)

		level.setBlockAndUpdate(growthPosition, growthState)
		level.scheduleTick(growthPosition, this, GROWTH_DELAY)

		val soundType = growthState.getSoundType(level, growthPosition, null)
		level.playSound(
			null,
			growthPosition,
			soundType.placeSound,
			SoundSource.BLOCKS,
			(soundType.volume + 1f) / 2f,
			soundType.pitch * 0.8f
		)
	}

	override fun createBlockStateDefinition(
		builder: StateDefinition.Builder<Block, BlockState>
	) {
		super.createBlockStateDefinition(builder)
		builder.add(
			NORTH_DISABLED,
			EAST_DISABLED,
			SOUTH_DISABLED,
			WEST_DISABLED,
			UP_DISABLED,
			DOWN_DISABLED,
			GROWTH_REMAINING
		)
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
		if (!stack.isItem(Items.BONE_MEAL)) {
			return super.useItemOn(stack, state, level, position, player, hand, hitResult)
		}

		val growthDirection = hitResult.direction
		val growthPosition = position.relative(growthDirection)
		if (!level.getBlockState(growthPosition).canBeReplaced()) {
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
		}

		if (level is ServerLevel) {
			stack.consume(1, player)

			if (level.random.roll(INITIAL_GROWTH_CHANCE)) {
				val growthRemaining = level.random.nextIntBetweenInclusive(
					MIN_INITIAL_GROWTH,
					MAX_INITIAL_GROWTH
				)

				grow(level, position, growthDirection, growthRemaining)
			}
		}

		return ItemInteractionResult.sidedSuccess(level.isClientSide)
	}

	override fun tick(
		state: BlockState,
		level: ServerLevel,
		position: BlockPos,
		random: RandomSource
	) {
		val growthRemaining = state.getValue(GROWTH_REMAINING)
		if (growthRemaining <= 0) return

		level.setBlockAndUpdate(position, state.setValue(GROWTH_REMAINING, 0))

		val growthCost = random.nextIntBetweenInclusive(MIN_GROWTH_COST, MAX_GROWTH_COST)
		val nextGrowthRemaining = growthRemaining - growthCost
		if (nextGrowthRemaining <= 0) return

		val growthDirection = getGrowthDirection(state, level, position, random)
		if (random.roll(PARALLEL_GROWTH_CHANCE)) {
			grow(level, position, growthDirection, nextGrowthRemaining)
			return
		}

		val forkDirection = getPerpendicularDirection(growthDirection, random)
		val firstGrowthRemaining = random.nextIntBetweenInclusive(MIN_BRANCH_GROWTH, nextGrowthRemaining)

		grow(level, position, forkDirection, firstGrowthRemaining)
		grow(level, position, forkDirection.opposite, nextGrowthRemaining - firstGrowthRemaining)
	}

	private fun getGrowthDirection(
		state: BlockState,
		level: Level,
		position: BlockPos,
		random: RandomSource
	): Direction {
		val positiveDirection = Direction.get(Direction.AxisDirection.POSITIVE, state.getValue(AXIS))
		val negativeDirection = positiveDirection.opposite
		val positiveIsStem = level.getBlockState(position.relative(positiveDirection)).isBlock(this)
		val negativeIsStem = level.getBlockState(position.relative(negativeDirection)).isBlock(this)

		if (positiveIsStem && !negativeIsStem) return negativeDirection
		if (negativeIsStem && !positiveIsStem) return positiveDirection

		return if (random.nextBoolean()) positiveDirection else negativeDirection
	}

	private fun getPerpendicularDirection(
		growthDirection: Direction,
		random: RandomSource
	): Direction {
		var direction = Direction.getRandom(random)
		while (direction.axis == growthDirection.axis) {
			direction = Direction.getRandom(random)
		}

		return direction
	}

	override fun getToolModifiedState(
		state: BlockState,
		context: UseOnContext,
		itemAbility: ItemAbility,
		simulate: Boolean
	): BlockState? {
		if (itemAbility != ItemAbilities.AXE_STRIP) return null

		val disabledProperty = getDisabledProperty(context.clickedFace)
		return state.setValue(disabledProperty, !state.getValue(disabledProperty))
	}

	override fun canAttachToBlock(
		blockState: BlockState,
		scoochworm: ScoochwormEntity,
		level: Level,
		position: BlockPos,
		supportDirection: Direction,
		fromDirection: Direction
	): Boolean {
		return !blockState.getValue(getDisabledProperty(supportDirection))
	}

	override fun getFlammability(state: BlockState, level: BlockGetter, position: BlockPos, direction: Direction): Int = 5
	override fun getFireSpreadSpeed(state: BlockState, level: BlockGetter, position: BlockPos, direction: Direction): Int = 5

	companion object {
		val NORTH_DISABLED: BooleanProperty = BooleanProperty.create("north_disabled")
		val EAST_DISABLED: BooleanProperty = BooleanProperty.create("east_disabled")
		val SOUTH_DISABLED: BooleanProperty = BooleanProperty.create("south_disabled")
		val WEST_DISABLED: BooleanProperty = BooleanProperty.create("west_disabled")
		val UP_DISABLED: BooleanProperty = BooleanProperty.create("up_disabled")
		val DOWN_DISABLED: BooleanProperty = BooleanProperty.create("down_disabled")
		val GROWTH_REMAINING: IntegerProperty = IntegerProperty.create("growth_remaining", 0, 40)

		private const val GROWTH_DELAY = 2
		private const val INITIAL_GROWTH_CHANCE = 0.45f
		private const val PARALLEL_GROWTH_CHANCE = 0.8f
		const val MIN_INITIAL_GROWTH = 30
		const val MAX_INITIAL_GROWTH = 40
		private const val MIN_GROWTH_COST = 4
		private const val MAX_GROWTH_COST = 8
		private const val MIN_BRANCH_GROWTH = 1

		fun getDisabledProperty(direction: Direction): BooleanProperty {
			return when (direction) {
				Direction.NORTH -> NORTH_DISABLED
				Direction.EAST -> EAST_DISABLED
				Direction.SOUTH -> SOUTH_DISABLED
				Direction.WEST -> WEST_DISABLED
				Direction.UP -> UP_DISABLED
				Direction.DOWN -> DOWN_DISABLED
			}
		}
	}
}