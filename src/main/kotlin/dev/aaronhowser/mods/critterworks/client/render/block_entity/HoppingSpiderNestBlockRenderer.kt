package dev.aaronhowser.mods.critterworks.client.render.block_entity

import com.mojang.blaze3d.vertex.PoseStack
import dev.aaronhowser.mods.aaron.misc.AaronDsls.withPose
import dev.aaronhowser.mods.critterworks.block_entity.HoppingSpiderNestBlockEntity
import dev.aaronhowser.mods.critterworks.handler.spider.HoppingSpider
import dev.aaronhowser.mods.critterworks.registry.ModItems
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.LevelRenderer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.BlockPos
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.joml.Matrix3f
import org.joml.Quaternionf
import kotlin.math.cos
import kotlin.math.sin

class HoppingSpiderNestBlockRenderer(
	context: BlockEntityRendererProvider.Context
) : BlockEntityRenderer<HoppingSpiderNestBlockEntity> {

	private val hoppingSpiderStack by lazy { ModItems.HOPPING_SPIDER.get().defaultInstance }

	override fun render(
		blockEntity: HoppingSpiderNestBlockEntity,
		partialTick: Float,
		poseStack: PoseStack,
		bufferSource: MultiBufferSource,
		packedLight: Int,
		packedOverlay: Int
	) {
		val level = blockEntity.level ?: return

		for (index in blockEntity.hoppingSpiders.indices) {
			val spider = blockEntity.hoppingSpiders[index]

			val position = getPosition(blockEntity, spider, level.gameTime, partialTick)
			val direction = spider.route?.getDirection(level.gameTime, partialTick)
			val spiderUp = if (direction == null) null else getSpiderUp(spider, direction)

			val displayOffset = spiderUp?.scale(DISTANCE_FROM_LINE) ?: getDisplayOffset(index)
			val displayPosition = position.add(displayOffset)
			val localPosition = displayPosition.subtract(Vec3.atLowerCornerOf(blockEntity.blockPos))

			val spiderLight = LevelRenderer.getLightColor(level, BlockPos.containing(displayPosition))

			poseStack.withPose {
				poseStack.translate(localPosition.x, localPosition.y, localPosition.z)

				if (direction != null && spiderUp != null) {
					poseStack.mulPose(getTravelRotation(direction, spiderUp))
				}

				renderSpider(spider, poseStack, bufferSource, spiderLight, packedOverlay)
			}
		}
	}

	private fun getTravelRotation(direction: Vec3, spiderUp: Vec3): Quaternionf {
		val backward = direction.scale(-1.0)
		val right = spiderUp.cross(backward).normalize()
		val rotationMatrix = Matrix3f()
			.setColumn(0, right.toVector3f())
			.setColumn(1, spiderUp.toVector3f())
			.setColumn(2, backward.toVector3f())

		return Quaternionf().setFromNormalized(rotationMatrix)
	}

	private fun getSpiderUp(spider: HoppingSpider, travelDirection: Vec3): Vec3 {
		val lineDirection = getConsistentLineDirection(travelDirection)
		var startingUp = UP.subtract(lineDirection.scale(UP.dot(lineDirection)))

		if (startingUp.lengthSqr() < MIN_DIRECTION_LENGTH_SQUARED) {
			startingUp = NORTH.subtract(lineDirection.scale(NORTH.dot(lineDirection)))
		}

		startingUp = startingUp.normalize()
		val side = lineDirection.cross(startingUp).normalize()
		val angle = Math.toRadians((spider.uuid.hashCode() % 360).toDouble())

		return startingUp.scale(cos(angle))
			.add(side.scale(sin(angle)))
	}

	private fun getConsistentLineDirection(direction: Vec3): Vec3 {
		if (direction.x < 0.0) return direction.scale(-1.0)
		if (direction.x > 0.0) return direction
		if (direction.y < 0.0) return direction.scale(-1.0)
		if (direction.y > 0.0) return direction
		if (direction.z < 0.0) return direction.scale(-1.0)

		return direction
	}

	private fun getPosition(
		blockEntity: HoppingSpiderNestBlockEntity,
		spider: HoppingSpider,
		gameTime: Long,
		partialTick: Float
	): Vec3 {
		return spider.getRenderPosition(gameTime, partialTick)
			?: blockEntity.blockPos.center
	}

	private fun renderSpider(
		spider: HoppingSpider,
		poseStack: PoseStack,
		bufferSource: MultiBufferSource,
		packedLight: Int,
		packedOverlay: Int
	) {
		poseStack.withPose {
			poseStack.scale(
				HOPPING_SPIDER_SCALE,
				HOPPING_SPIDER_SCALE,
				HOPPING_SPIDER_SCALE
			)

			Minecraft.getInstance().itemRenderer.renderStatic(
				hoppingSpiderStack,
				ItemDisplayContext.GROUND,
				packedLight,
				packedOverlay,
				poseStack,
				bufferSource,
				Minecraft.getInstance().level,
				0
			)
		}

		if (spider.carriedStack.isEmpty) return

		poseStack.withPose {
			poseStack.translate(0.0, ITEM_HEIGHT, 0.0)
			poseStack.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE)

			Minecraft.getInstance().itemRenderer.renderStatic(
				spider.carriedStack,
				ItemDisplayContext.GROUND,
				packedLight,
				packedOverlay,
				poseStack,
				bufferSource,
				Minecraft.getInstance().level,
				0
			)
		}
	}

	private fun getDisplayOffset(index: Int): Vec3 {
		val offsetX = if (index % 2 == 0) -IDLE_OFFSET else IDLE_OFFSET
		val offsetZ = if (index / 2 == 0) -IDLE_OFFSET else IDLE_OFFSET

		return Vec3(offsetX, 0.0, offsetZ)
	}

	override fun getRenderBoundingBox(blockEntity: HoppingSpiderNestBlockEntity): AABB {
		return AABB.INFINITE
	}

	override fun shouldRenderOffScreen(blockEntity: HoppingSpiderNestBlockEntity): Boolean {
		return true
	}

	companion object {
		private const val HOPPING_SPIDER_SCALE = 0.5f
		private const val ITEM_SCALE = 0.6f
		private const val ITEM_HEIGHT = 0.18
		private const val IDLE_OFFSET = 0.14
		private const val DISTANCE_FROM_LINE = 0.04
		private const val MIN_DIRECTION_LENGTH_SQUARED = 1.0e-6

		private val UP = Vec3(0.0, 1.0, 0.0)
		private val NORTH = Vec3(0.0, 0.0, -1.0)
	}
}