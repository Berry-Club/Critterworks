package dev.aaronhowser.mods.critterworks.block

import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isBlock
import dev.aaronhowser.mods.critterworks.registry.ModItems
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.material.FluidState
import net.minecraft.world.level.material.MapColor
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import java.util.concurrent.ConcurrentHashMap

class SnailSlimeBlock : HoneyBlock(
	Properties
		.of()
		.mapColor(MapColor.COLOR_BROWN)
		.sound(SoundType.HONEY_BLOCK)
		.jumpFactor(1f)
		.speedFactor(1f)
		.noCollission()
		.strength(0.1f)
) {

	init {
		registerDefaultState(
			defaultBlockState()
				.setValue(HAS_DOWN, false)
				.setValue(HAS_UP, false)
				.setValue(HAS_NORTH, false)
				.setValue(HAS_SOUTH, false)
				.setValue(HAS_WEST, false)
				.setValue(HAS_EAST, false)
		)
	}

	override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
		super.createBlockStateDefinition(builder)
		builder.add(HAS_DOWN, HAS_UP, HAS_NORTH, HAS_SOUTH, HAS_WEST, HAS_EAST)
	}

	override fun onDestroyedByPlayer(
		state: BlockState,
		level: Level,
		position: BlockPos,
		player: Player,
		willHarvest: Boolean,
		fluid: FluidState
	): Boolean {
		val removedDirection = getTargetedDirection(state, position, player)
		val removedProperty = PipeBlock.PROPERTY_BY_DIRECTION.getValue(removedDirection)
		val remainingState = state.setValue(removedProperty, false)

		val hasFaces = PipeBlock.PROPERTY_BY_DIRECTION
			.values
			.any(remainingState::getValue)

		if (hasFaces) {
			return level.setBlock(position, remainingState, UPDATE_ALL_IMMEDIATE)
		}

		return level.setBlock(position, fluid.createLegacyBlock(), UPDATE_ALL_IMMEDIATE)
	}

	override fun playerDestroy(
		level: Level,
		player: Player,
		position: BlockPos,
		state: BlockState,
		blockEntity: BlockEntity?,
		tool: ItemStack
	) {
		super.playerDestroy(level, player, position, state, blockEntity, tool)
		popResource(level, position, ModItems.SNAIL_SLIME.toStack())
	}

	override fun getShape(
		state: BlockState,
		level: BlockGetter,
		position: BlockPos,
		context: CollisionContext
	): VoxelShape {
		val packedDirections = packDirections(state)
		val cachedShape = SHAPES_BY_PACKED_DIRECTIONS[packedDirections]
		if (cachedShape != null) return cachedShape

		var shape = Shapes.empty()

		if (state.getValue(HAS_DOWN)) shape = Shapes.or(shape, SHAPE_DOWN)
		if (state.getValue(HAS_UP)) shape = Shapes.or(shape, SHAPE_UP)
		if (state.getValue(HAS_NORTH)) shape = Shapes.or(shape, SHAPE_NORTH)
		if (state.getValue(HAS_SOUTH)) shape = Shapes.or(shape, SHAPE_SOUTH)
		if (state.getValue(HAS_WEST)) shape = Shapes.or(shape, SHAPE_WEST)
		if (state.getValue(HAS_EAST)) shape = Shapes.or(shape, SHAPE_EAST)

		SHAPES_BY_PACKED_DIRECTIONS[packedDirections] = shape.optimize()

		return shape
	}

	override fun getCollisionShape(
		state: BlockState,
		level: BlockGetter,
		position: BlockPos,
		context: CollisionContext
	): VoxelShape {
		return getShape(state, level, position, context)
	}

	override fun skipRendering(state: BlockState, adjacentState: BlockState, side: Direction): Boolean {
		if (adjacentState.isBlock(this)) return true

		return super.skipRendering(state, adjacentState, side)
	}

	override fun updateShape(
		state: BlockState,
		direction: Direction,
		neighborState: BlockState,
		level: LevelAccessor,
		pos: BlockPos,
		neighborPos: BlockPos
	): BlockState {
		var updatedState = state

		for (direction in Direction.entries) {
			val property = PipeBlock.PROPERTY_BY_DIRECTION[direction] ?: continue
			if (!updatedState.getValue(property)) continue

			val supportPosition = pos.relative(direction)
			val supportState = level.getBlockState(supportPosition)

			if (MultifaceBlock.canAttachTo(level, direction, supportPosition, supportState)) continue

			updatedState = updatedState.setValue(property, false)
		}

		for (direction in Direction.entries) {
			val property = PipeBlock.PROPERTY_BY_DIRECTION[direction] ?: continue
			if (updatedState.getValue(property)) return updatedState
		}

		return Blocks.AIR.defaultBlockState()
	}

	override fun isSlidingDown(position: BlockPos, entity: Entity): Boolean {
		if (entity.onGround()) return false

		val state = entity.level().getBlockState(position)
		return isTouchingSlimeSide(state, position, entity)
	}

	private fun isTouchingSlimeSide(state: BlockState, position: BlockPos, entity: Entity): Boolean {
		if (entity.deltaMovement.y >= -0.08) return false

		val distance = 0.0625
		val boundingBox = entity.boundingBox

		if (state.getValue(HAS_WEST)) {
			val distanceToWestFace = boundingBox.minX - position.x
			if (distanceToWestFace <= distance) return true
		}

		if (state.getValue(HAS_EAST)) {
			val distanceToEastFace = (position.x + 1.0) - boundingBox.maxX
			if (distanceToEastFace <= distance) return true
		}

		if (state.getValue(HAS_NORTH)) {
			val distanceToNorthFace = boundingBox.minZ - position.z
			if (distanceToNorthFace <= distance) return true
		}

		if (state.getValue(HAS_SOUTH)) {
			val distanceToSouthFace = (position.z + 1.0) - boundingBox.maxZ
			if (distanceToSouthFace <= distance) return true
		}

		return false
	}

	private fun getTargetedDirection(state: BlockState, position: BlockPos, player: Player): Direction {
		val lookStart = player.eyePosition
		val lookEnd = lookStart.add(player.lookAngle.scale(player.blockInteractionRange()))

		var closestDirection: Direction? = null
		var closestDistance = Double.MAX_VALUE

		for (direction in Direction.entries) {
			val property = PipeBlock.PROPERTY_BY_DIRECTION.getValue(direction)
			if (!state.getValue(property)) continue

			val shape = SHAPE_BY_DIRECTION.getValue(direction)
			val hitResult = shape.clip(lookStart, lookEnd, position) ?: continue
			val distance = lookStart.distanceToSqr(hitResult.location)

			if (distance >= closestDistance) continue

			closestDirection = direction
			closestDistance = distance
		}

		if (closestDirection != null) return closestDirection

		for (direction in Direction.entries) {
			val property = PipeBlock.PROPERTY_BY_DIRECTION.getValue(direction)
			if (state.getValue(property)) return direction
		}

		error("Snail slime block has no faces")
	}

	fun getStateForPlacement(
		existingState: BlockState,
		level: BlockGetter,
		position: BlockPos,
		face: Direction
	): BlockState? {
		val faceProperty = MultifaceBlock.getFaceProperty(face)
		val supportPosition = position.relative(face)
		val supportState = level.getBlockState(supportPosition)

		when {
			existingState.isBlock(this) && existingState.getValue(faceProperty) -> return null
			!existingState.isBlock(this) && !existingState.canBeReplaced() -> return null
			!MultifaceBlock.canAttachTo(level, face, supportPosition, supportState) -> return null
		}

		val state = if (existingState.isBlock(this)) existingState else defaultBlockState()
		return state.setValue(faceProperty, true)
	}

	companion object {
		private const val DOWN_BIT = 1
		private const val UP_BIT = 1 shl 1
		private const val NORTH_BIT = 1 shl 2
		private const val SOUTH_BIT = 1 shl 3
		private const val WEST_BIT = 1 shl 4
		private const val EAST_BIT = 1 shl 5

		private val SHAPES_BY_PACKED_DIRECTIONS: MutableMap<Int, VoxelShape> = ConcurrentHashMap()

		val HAS_DOWN: BooleanProperty = BlockStateProperties.DOWN
		val HAS_UP: BooleanProperty = BlockStateProperties.UP
		val HAS_NORTH: BooleanProperty = BlockStateProperties.NORTH
		val HAS_SOUTH: BooleanProperty = BlockStateProperties.SOUTH
		val HAS_WEST: BooleanProperty = BlockStateProperties.WEST
		val HAS_EAST: BooleanProperty = BlockStateProperties.EAST

		val SHAPE_DOWN: VoxelShape = box(0.0, 0.0, 0.0, 16.0, 1.0, 16.0)
		val SHAPE_UP: VoxelShape = box(0.0, 15.0, 0.0, 16.0, 16.0, 16.0)
		val SHAPE_NORTH: VoxelShape = box(0.0, 0.0, 0.0, 16.0, 16.0, 1.0)
		val SHAPE_SOUTH: VoxelShape = box(0.0, 0.0, 15.0, 16.0, 16.0, 16.0)
		val SHAPE_WEST: VoxelShape = box(0.0, 0.0, 0.0, 1.0, 16.0, 16.0)
		val SHAPE_EAST: VoxelShape = box(15.0, 0.0, 0.0, 16.0, 16.0, 16.0)

		val SHAPE_BY_DIRECTION: Map<Direction, VoxelShape> = mapOf(
			Direction.DOWN to SHAPE_DOWN,
			Direction.UP to SHAPE_UP,
			Direction.NORTH to SHAPE_NORTH,
			Direction.SOUTH to SHAPE_SOUTH,
			Direction.WEST to SHAPE_WEST,
			Direction.EAST to SHAPE_EAST
		)

		private fun packDirections(state: BlockState): Int {
			var packedDirections = 0

			if (state.getValue(HAS_DOWN)) packedDirections = packedDirections or DOWN_BIT
			if (state.getValue(HAS_UP)) packedDirections = packedDirections or UP_BIT
			if (state.getValue(HAS_NORTH)) packedDirections = packedDirections or NORTH_BIT
			if (state.getValue(HAS_SOUTH)) packedDirections = packedDirections or SOUTH_BIT
			if (state.getValue(HAS_WEST)) packedDirections = packedDirections or WEST_BIT
			if (state.getValue(HAS_EAST)) packedDirections = packedDirections or EAST_BIT

			return packedDirections
		}
	}

}