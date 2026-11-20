package dev.aaronhowser.mods.critterworks.block

import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isBlock
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.HoneyBlock
import net.minecraft.world.level.block.MultifaceBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.material.MapColor
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import java.util.concurrent.ConcurrentHashMap

class SnailSlimeBlock : HoneyBlock(
	Properties
		.ofFullCopy(Blocks.HONEY_BLOCK)
		.mapColor(MapColor.COLOR_BROWN)
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

		SHAPES_BY_PACKED_DIRECTIONS[packedDirections] = shape

		return shape
	}

	override fun getCollisionShape(
		state: BlockState,
		level: BlockGetter,
		position: BlockPos,
		context: CollisionContext
	): VoxelShape {
		val packedDirections = packDirections(state)
		val cachedShape = COLLISION_SHAPES_BY_PACKED_DIRECTIONS[packedDirections]
		if (cachedShape != null) return cachedShape

		var shape = Shapes.empty()

		if (state.getValue(HAS_DOWN)) shape = Shapes.or(shape, COLLISION_SHAPE_DOWN)
		if (state.getValue(HAS_UP)) shape = Shapes.or(shape, COLLISION_SHAPE_UP)
		if (state.getValue(HAS_NORTH)) shape = Shapes.or(shape, COLLISION_SHAPE_NORTH)
		if (state.getValue(HAS_SOUTH)) shape = Shapes.or(shape, COLLISION_SHAPE_SOUTH)
		if (state.getValue(HAS_WEST)) shape = Shapes.or(shape, COLLISION_SHAPE_WEST)
		if (state.getValue(HAS_EAST)) shape = Shapes.or(shape, COLLISION_SHAPE_EAST)

		COLLISION_SHAPES_BY_PACKED_DIRECTIONS[packedDirections] = shape

		return shape
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

		if (existingState.isBlock(this) && existingState.getValue(faceProperty)) return null
		if (!existingState.isBlock(this) && !existingState.canBeReplaced()) return null
		if (!MultifaceBlock.canAttachTo(level, face, supportPosition, supportState)) return null

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
		private val COLLISION_SHAPES_BY_PACKED_DIRECTIONS: MutableMap<Int, VoxelShape> = ConcurrentHashMap()

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

		val COLLISION_SHAPE_DOWN: VoxelShape = box(0.0, 0.25, 0.0, 16.0, 1.0, 16.0)
		val COLLISION_SHAPE_UP: VoxelShape = box(0.0, 15.0, 0.0, 16.0, 15.75, 16.0)
		val COLLISION_SHAPE_NORTH: VoxelShape = box(0.0, 0.0, 0.25, 16.0, 16.0, 1.0)
		val COLLISION_SHAPE_SOUTH: VoxelShape = box(0.0, 0.0, 15.0, 16.0, 16.0, 15.75)
		val COLLISION_SHAPE_WEST: VoxelShape = box(0.25, 0.0, 0.0, 1.0, 16.0, 16.0)
		val COLLISION_SHAPE_EAST: VoxelShape = box(15.0, 0.0, 0.0, 15.75, 16.0, 16.0)

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