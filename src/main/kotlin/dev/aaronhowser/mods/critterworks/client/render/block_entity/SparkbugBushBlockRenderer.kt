package dev.aaronhowser.mods.critterworks.client.render.block_entity

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import dev.aaronhowser.mods.aaron.client.render.AaronRenderTypes
import dev.aaronhowser.mods.aaron.client.render.AaronRenderUtil
import dev.aaronhowser.mods.critterworks.block_entity.SparkbugBushBlockEntity
import dev.aaronhowser.mods.critterworks.registry.ModDataComponents
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.phys.AABB

class SparkbugBushBlockRenderer(
	context: BlockEntityRendererProvider.Context
) : BlockEntityRenderer<SparkbugBushBlockEntity> {

	override fun render(
		blockEntity: SparkbugBushBlockEntity,
		partialTick: Float,
		poseStack: PoseStack,
		bufferSource: MultiBufferSource,
		packedLight: Int,
		packedOverlay: Int
	) {
		val player = Minecraft.getInstance().player ?: return
		if (!player.isHolding { it.get(ModDataComponents.SPARKBUG_BUSH) == blockEntity.blockPos.asLong() }) return

		val vertexConsumer = bufferSource.getBuffer(AaronRenderTypes.QUADS_THROUGH_WALLS)
		val pose = poseStack.last()

		drawSpots(vertexConsumer, pose, blockEntity.blockPos, blockEntity.inputPollenSpots, INPUT_COLOR)
		drawSpots(vertexConsumer, pose, blockEntity.blockPos, blockEntity.outputPollenSpots, OUTPUT_COLOR)
	}

	private fun drawSpots(
		vertexConsumer: VertexConsumer,
		pose: PoseStack.Pose,
		blockPos: BlockPos,
		pollenSpots: List<SparkbugBushBlockEntity.PollenSpot>,
		color: Int
	) {
		for (pollenSpot in pollenSpots) {
			val blockOffset = pollenSpot.pos.subtract(blockPos)
			val vertices = AaronRenderUtil.getVertices(pollenSpot.direction, 1f, 1f, 1f)

			for (vertex in vertices) {
				when (pollenSpot.direction.axis) {
					Direction.Axis.X -> {
						vertex.x += pollenSpot.direction.stepX * PLANE_OFFSET
						vertex.y = FACE_INSET + vertex.y * FACE_SIZE
						vertex.z = FACE_INSET + vertex.z * FACE_SIZE
					}

					Direction.Axis.Y -> {
						vertex.x = FACE_INSET + vertex.x * FACE_SIZE
						vertex.y += pollenSpot.direction.stepY * PLANE_OFFSET
						vertex.z = FACE_INSET + vertex.z * FACE_SIZE
					}

					Direction.Axis.Z -> {
						vertex.x = FACE_INSET + vertex.x * FACE_SIZE
						vertex.y = FACE_INSET + vertex.y * FACE_SIZE
						vertex.z += pollenSpot.direction.stepZ * PLANE_OFFSET
					}
				}

				AaronRenderUtil.addColoredVertex(
					pose,
					vertexConsumer,
					color,
					vertex.x + blockOffset.x,
					vertex.y + blockOffset.y,
					vertex.z + blockOffset.z
				)
			}
		}
	}

	override fun getRenderBoundingBox(blockEntity: SparkbugBushBlockEntity): AABB {
		var bounds = AABB(blockEntity.blockPos)

		for (pollenSpot in blockEntity.inputPollenSpots) {
			bounds = bounds.minmax(AABB(pollenSpot.pos))
		}

		for (pollenSpot in blockEntity.outputPollenSpots) {
			bounds = bounds.minmax(AABB(pollenSpot.pos))
		}

		return bounds.inflate(PLANE_OFFSET.toDouble())
	}

	companion object {
		private const val FACE_SIZE = 0.5f
		private const val FACE_INSET = (1f - FACE_SIZE) / 2f
		private const val PLANE_OFFSET = 0.002f

		private const val INPUT_COLOR = 0x600066FF
		private const val OUTPUT_COLOR = 0x60FF8000
	}
}