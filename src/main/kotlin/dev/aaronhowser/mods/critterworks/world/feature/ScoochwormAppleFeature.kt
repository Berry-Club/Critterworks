package dev.aaronhowser.mods.critterworks.world.feature

import dev.aaronhowser.mods.aaron.misc.AaronExtensions.nextRange
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.withClickToRunCommand
import dev.aaronhowser.mods.critterworks.block.ScoochstemBlock
import dev.aaronhowser.mods.critterworks.config.ServerConfig
import dev.aaronhowser.mods.critterworks.registry.ModBlocks
import dev.aaronhowser.mods.critterworks.registry.ModEntityTypes
import dev.aaronhowser.mods.critterworks.world.feature.config.ScoochwormAppleConfiguration
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.world.entity.MobSpawnType
import net.minecraft.world.level.WorldGenLevel
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.levelgen.feature.Feature
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext

class ScoochwormAppleFeature : Feature<ScoochwormAppleConfiguration>(ScoochwormAppleConfiguration.CODEC) {

	override fun place(context: FeaturePlaceContext<ScoochwormAppleConfiguration>): Boolean {
		val level = context.level()
		val configuration = context.config()
		val rarity = ServerConfig.CONFIG.scoochwormAppleRarity.get()
		if (context.random().nextRange(0, rarity) != 0) return false

		val radius = configuration.radius.sample(context.random())
		val floorPosition = findFloor(level, context.origin(), configuration.verticalSearchRange) ?: return false
		val center = floorPosition.above(radius)

		if (!isExposed(level, center, radius)) return false

		placeApple(level, center, radius)

		if (configuration.spawnScoochworm) {
			spawnScoochworm(level, center.below(radius))
		}
//		sendTeleportMessage(level, center)

		return true
	}

	private fun findFloor(level: WorldGenLevel, origin: BlockPos, verticalSearchRange: Int): BlockPos? {
		var position = origin
		var tries = 0

		while (tries < verticalSearchRange && !level.isEmptyBlock(position)) {
			position = position.above()
			tries++
		}

		if (!level.isEmptyBlock(position)) return null

		while (tries < verticalSearchRange * 2 && level.isEmptyBlock(position.below())) {
			position = position.below()
			tries++
		}

		if (level.isEmptyBlock(position.below())) return null

		return position.below()
	}

	private fun isExposed(level: WorldGenLevel, center: BlockPos, radius: Int): Boolean {
		val mutablePosition = BlockPos.MutableBlockPos()

		for (xOffset in -radius..radius) {
			for (yOffset in -radius..radius) {
				for (zOffset in -radius..radius) {
					val internalFace = getInternalFace(xOffset, yOffset, zOffset, radius) ?: continue

					mutablePosition.setWithOffset(center, xOffset, yOffset, zOffset)
					mutablePosition.move(internalFace.opposite)

					if (level.isEmptyBlock(mutablePosition)) return true
				}
			}
		}

		return false
	}

	private fun placeApple(level: WorldGenLevel, center: BlockPos, radius: Int) {
		val mutablePosition = BlockPos.MutableBlockPos()

		for (xOffset in -radius..radius) {
			for (yOffset in -radius..radius) {
				for (zOffset in -radius..radius) {
					mutablePosition.setWithOffset(center, xOffset, yOffset, zOffset)

					val internalFace = getInternalFace(xOffset, yOffset, zOffset, radius)
					val blockState = if (internalFace == null) {
						Blocks.AIR.defaultBlockState()
					} else {
						appleStateWithDisabledFace(internalFace)
					}

					level.setBlock(mutablePosition, blockState, Block.UPDATE_CLIENTS)
				}
			}
		}

		val stemPos = center.above(radius + 1)
		val stemState = ModBlocks.SCOOCHSTEM.get()
			.defaultBlockState()
			.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y)
			.setValue(
				ScoochstemBlock.GROWTH_REMAINING,
				level.random.nextIntBetweenInclusive(ScoochstemBlock.MIN_INITIAL_GROWTH, ScoochstemBlock.MAX_INITIAL_GROWTH)
			)

		level.setBlock(stemPos, stemState, Block.UPDATE_CLIENTS)
		level.scheduleTick(stemPos, stemState.block, 3)

		val berryPos = center.above(radius - 1)
		val berryState = Blocks.CAVE_VINES
			.defaultBlockState()
			.setValue(CaveVinesBlock.BERRIES, true)

		level.setBlock(berryPos, berryState, Block.UPDATE_CLIENTS)

	}

	private fun spawnScoochworm(level: WorldGenLevel, floorPosition: BlockPos) {
		val scoochworm = ModEntityTypes.SCOOCHWORM.get()
			.spawn(
				level.level,
				floorPosition.above(),
				MobSpawnType.STRUCTURE
			) ?: return

		scoochworm.attachToSupport(floorPosition, Direction.DOWN)
		scoochworm.setPersistenceRequired()
		scoochworm.isTryingToMove = true
	}

	private fun sendTeleportMessage(level: WorldGenLevel, position: BlockPos) {
		val message = Component.literal("[${position.x}, ${position.y}, ${position.z}]")
			.withStyle(
				Style.EMPTY
					.withClickToRunCommand("/tp @s ${position.x} ${position.y} ${position.z}")
			)

		level.level.server.playerList.broadcastSystemMessage(message, false)
	}

	private fun getInternalFace(xOffset: Int, yOffset: Int, zOffset: Int, radius: Int): Direction? {
		var internalFace: Direction? = null
		var boundaryCount = 0

		if (xOffset == -radius || xOffset == radius) {
			internalFace = if (xOffset < 0) Direction.EAST else Direction.WEST
			boundaryCount++
		}

		if (yOffset == -radius || yOffset == radius) {
			internalFace = if (yOffset < 0) Direction.UP else Direction.DOWN
			boundaryCount++
		}

		if (zOffset == -radius || zOffset == radius) {
			internalFace = if (zOffset < 0) Direction.SOUTH else Direction.NORTH
			boundaryCount++
		}

		return if (boundaryCount == 1) internalFace else null
	}

	private fun appleStateWithDisabledFace(internalFace: Direction): BlockState {
		val property: BooleanProperty = when (internalFace) {
			Direction.NORTH -> HugeMushroomBlock.NORTH
			Direction.EAST -> HugeMushroomBlock.EAST
			Direction.SOUTH -> HugeMushroomBlock.SOUTH
			Direction.WEST -> HugeMushroomBlock.WEST
			Direction.UP -> HugeMushroomBlock.UP
			Direction.DOWN -> HugeMushroomBlock.DOWN
		}

		return ModBlocks.APPLE_SLICE.get()
			.defaultBlockState()
			.setValue(property, false)
	}

}