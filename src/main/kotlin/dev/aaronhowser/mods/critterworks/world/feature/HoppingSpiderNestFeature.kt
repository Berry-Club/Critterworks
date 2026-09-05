package dev.aaronhowser.mods.critterworks.world.feature

import dev.aaronhowser.mods.aaron.misc.AaronExtensions.nextRange
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.withClickToRunCommand
import dev.aaronhowser.mods.critterworks.block_entity.HoppingSpiderNestBlockEntity
import dev.aaronhowser.mods.critterworks.config.ServerConfig
import dev.aaronhowser.mods.critterworks.handler.spider.HoppingSpider
import dev.aaronhowser.mods.critterworks.handler.web.WebLineInteractionHandler
import dev.aaronhowser.mods.critterworks.handler.web.WebSavedData
import dev.aaronhowser.mods.critterworks.handler.web.line.WebLine
import dev.aaronhowser.mods.critterworks.handler.web.node.WebBlockAnchor
import dev.aaronhowser.mods.critterworks.handler.web.node.WebLineAnchor
import dev.aaronhowser.mods.critterworks.handler.web.node.WebNode
import dev.aaronhowser.mods.critterworks.registry.ModBlocks
import dev.aaronhowser.mods.critterworks.world.feature.config.HoppingSpiderNestConfiguration
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.util.RandomSource
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.WorldGenLevel
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.levelgen.Heightmap
import net.minecraft.world.level.levelgen.feature.Feature
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.CollisionContext
import java.util.UUID

class HoppingSpiderNestFeature : Feature<HoppingSpiderNestConfiguration>(HoppingSpiderNestConfiguration.CODEC) {

	override fun place(context: FeaturePlaceContext<HoppingSpiderNestConfiguration>): Boolean {
		val level = context.level()
		val configuration = context.config()
		val nestPosition = context.origin()
		val random = context.random()
		val rarity = ServerConfig.CONFIG.hoppingSpiderNestRarity.get()
		if (random.nextRange(0, rarity) != 0) return false

		val nestState = level.getBlockState(nestPosition)
		if (!nestState.isCollisionShapeFullBlock(level, nestPosition)) return false

		val exposedFaces = getExposedFaces(level, nestPosition)
		if (exposedFaces.isEmpty()) return false
		if (!hasSolidNeighbor(level, nestPosition)) return false
		if (!isUnderground(level, nestPosition, exposedFaces, configuration)) return false

		val rayHits = findSuitableRayHits(level, nestPosition, exposedFaces, random, configuration)
		if (rayHits.size < configuration.webCount.minInclusive) return false

		level.setBlock(
			nestPosition,
			ModBlocks.HOPPING_SPIDER_NEST.get().defaultBlockState(),
			Block.UPDATE_CLIENTS
		)

		populateNest(level, nestPosition, random, configuration)
		placeWebLines(level, rayHits, random, configuration)
		sendTeleportMessage(level, nestPosition)

		return true
	}

	private fun findSuitableRayHits(
		level: WorldGenLevel,
		nestPosition: BlockPos,
		exposedFaces: List<Direction>,
		random: RandomSource,
		configuration: HoppingSpiderNestConfiguration
	): List<Pair<WebBlockAnchor, BlockHitResult>> {
		val rayHits: MutableList<Pair<WebBlockAnchor, BlockHitResult>> = mutableListOf()
		val availableFaces = exposedFaces.toMutableList()

		for (rayIndex in 0 until configuration.rayAttempts) {
			if (rayHits.size >= configuration.webCount.maxInclusive || availableFaces.isEmpty()) break

			val faceIndex = random.nextRange(0, availableFaces.size)
			val face = availableFaces[faceIndex]

			val pointOnFace = randomPointOnFace(
				nestPosition,
				face,
				random,
				FACE_EDGE_INSET
			)
			val nestAnchor = WebLineInteractionHandler.createBlockAnchor(
				nestPosition,
				face,
				pointOnFace
			)
			val rayStart = nestAnchor.position
			val direction = randomDirection(face, random, configuration.raySpread)
			val hitResult = level.clip(
				ClipContext(
					rayStart,
					rayStart.add(direction.scale(configuration.webDistance.maxInclusive)),
					ClipContext.Block.COLLIDER,
					ClipContext.Fluid.NONE,
					CollisionContext.empty()
				)
			)

			if (hitResult.type != HitResult.Type.BLOCK) continue

			val distance = rayStart.distanceTo(hitResult.location)
			if (distance <= configuration.webDistance.minInclusive
				|| distance >= configuration.webDistance.maxInclusive
			) continue

			rayHits.add(nestAnchor to hitResult)
			availableFaces.removeAt(faceIndex)
		}

		return rayHits
	}

	private fun getExposedFaces(level: WorldGenLevel, position: BlockPos): List<Direction> {
		val exposedFaces: MutableList<Direction> = mutableListOf()

		for (direction in Direction.entries) {
			if (level.isEmptyBlock(position.relative(direction))) {
				exposedFaces.add(direction)
			}
		}

		return exposedFaces
	}

	private fun hasSolidNeighbor(level: WorldGenLevel, position: BlockPos): Boolean {
		for (direction in Direction.entries) {
			val neighborPosition = position.relative(direction)
			val neighborState = level.getBlockState(neighborPosition)
			if (neighborState.isCollisionShapeFullBlock(level, neighborPosition)) return true
		}

		return false
	}

	private fun isUnderground(
		level: WorldGenLevel,
		position: BlockPos,
		exposedFaces: List<Direction>,
		configuration: HoppingSpiderNestConfiguration
	): Boolean {
		val surfaceHeight = level.getHeight(
			Heightmap.Types.WORLD_SURFACE_WG,
			position.x,
			position.z
		)

		val depthBelowSurface = surfaceHeight - 1 - position.y
		if (depthBelowSurface < configuration.minimumDepthBelowSurface) return false

		for (face in exposedFaces) {
			val airPosition = position.relative(face)
			val airSurfaceHeight = level.getHeight(
				Heightmap.Types.WORLD_SURFACE_WG,
				airPosition.x,
				airPosition.z
			)

			if (airPosition.y >= airSurfaceHeight) return false
		}

		return true
	}

	private fun randomPointOnFace(
		position: BlockPos,
		face: Direction,
		random: RandomSource,
		edgeInset: Double
	): Vec3 {
		val center = position.center

		val maximumOffset = 0.5 - edgeInset
		val firstOffset = random.nextRange(-maximumOffset, maximumOffset)
		val secondOffset = random.nextRange(-maximumOffset, maximumOffset)

		return when (face.axis) {
			Direction.Axis.X -> Vec3(
				center.x + face.stepX * 0.5,
				center.y + firstOffset,
				center.z + secondOffset
			)

			Direction.Axis.Y -> Vec3(
				center.x + firstOffset,
				center.y + face.stepY * 0.5,
				center.z + secondOffset
			)

			Direction.Axis.Z -> Vec3(
				center.x + firstOffset,
				center.y + secondOffset,
				center.z + face.stepZ * 0.5
			)
		}
	}

	private fun randomDirection(face: Direction, random: RandomSource, spread: Double): Vec3 {
		val firstOffset = random.nextRange(-spread, spread)
		val secondOffset = random.nextRange(-spread, spread)

		return when (face.axis) {
			Direction.Axis.X -> Vec3(face.stepX.toDouble(), firstOffset, secondOffset)
			Direction.Axis.Y -> Vec3(firstOffset, face.stepY.toDouble(), secondOffset)
			Direction.Axis.Z -> Vec3(firstOffset, secondOffset, face.stepZ.toDouble())
		}.normalize()
	}

	private fun populateNest(
		level: WorldGenLevel,
		position: BlockPos,
		random: RandomSource,
		configuration: HoppingSpiderNestConfiguration
	) {
		val nest = level.getBlockEntity(position) as? HoppingSpiderNestBlockEntity ?: return
		val spiderCount = random.nextIntBetweenInclusive(
			configuration.spiders.minInclusive,
			configuration.spiders.maxInclusive
		)

		for (spiderIndex in 0 until spiderCount) {
			nest.hoppingSpiders.add(HoppingSpider())
		}

		nest.setChanged()
	}

	private fun placeWebLines(
		level: WorldGenLevel,
		rayHits: List<Pair<WebBlockAnchor, BlockHitResult>>,
		random: RandomSource,
		configuration: HoppingSpiderNestConfiguration
	) {
		val serverLevel = level.level
		val savedData = WebSavedData.get(serverLevel)
		val generatedLines: MutableList<Pair<WebLine, WebBlockAnchor>> = mutableListOf()

		for ((nestAnchor, rayHit) in rayHits) {
			val caveAnchor = WebLineInteractionHandler.createBlockAnchor(
				rayHit.blockPos,
				rayHit.direction,
				rayHit.location
			)

			val line = WebLine(UUID.randomUUID(), nestAnchor, caveAnchor)
			savedData.addLine(serverLevel, line)
			generatedLines.add(line to caveAnchor)
		}

		if (!configuration.connectWebs) return

		for (firstIndex in 0 until generatedLines.lastIndex) {
			for (secondIndex in firstIndex + 1 until generatedLines.size) {
				val first = generatedLines[firstIndex]
				val second = generatedLines[secondIndex]
				connectLines(level, savedData, first, second, random, configuration)
			}
		}
	}

	private fun connectLines(
		level: WorldGenLevel,
		savedData: WebSavedData,
		first: Pair<WebLine, WebBlockAnchor>,
		second: Pair<WebLine, WebBlockAnchor>,
		random: RandomSource,
		configuration: HoppingSpiderNestConfiguration
	) {
		val firstNode = createLineNode(first.first, random, configuration)
		val secondNode = createLineNode(second.first, random, configuration)
		val connections = listOf(
			firstNode to secondNode,
			firstNode to second.second,
			first.second to secondNode,
			first.second to second.second
		)

		for ((firstConnection, secondConnection) in connections) {
			if (!canConnect(level, firstConnection, secondConnection, configuration)) continue

			val line = WebLine(UUID.randomUUID(), firstConnection, secondConnection)
			savedData.addLine(level.level, line)
			return
		}
	}

	private fun createLineNode(
		line: WebLine,
		random: RandomSource,
		configuration: HoppingSpiderNestConfiguration
	): WebLineAnchor {
		val progress = random.nextRange(
			configuration.connectionProgress.minInclusive,
			configuration.connectionProgress.maxInclusive
		)
		val position = line.firstNode.position.lerp(line.secondNode.position, progress)

		return WebLineAnchor(UUID.randomUUID(), line.uuid, position)
	}

	private fun canConnect(
		level: WorldGenLevel,
		first: WebNode,
		second: WebNode,
		configuration: HoppingSpiderNestConfiguration
	): Boolean {
		val maximumDistance = configuration.webDistance.maxInclusive
		val maximumDistanceSquared = maximumDistance * maximumDistance
		if (first.position.distanceToSqr(second.position) >= maximumDistanceSquared) return false

		val hitResult = level.clip(
			ClipContext(
				first.position,
				second.position,
				ClipContext.Block.COLLIDER,
				ClipContext.Fluid.NONE,
				CollisionContext.empty()
			)
		)

		return hitResult.type == HitResult.Type.MISS
	}

	private fun sendTeleportMessage(level: WorldGenLevel, position: BlockPos) {
		val message = Component.literal("[${position.x}, ${position.y}, ${position.z}]")
			.withStyle(
				Style.EMPTY
					.withClickToRunCommand("/tp @s ${position.x} ${position.y} ${position.z}")
			)

		level.level.server.playerList.broadcastSystemMessage(message, false)
	}

	companion object {
		private const val FACE_EDGE_INSET = 0.2
	}

}