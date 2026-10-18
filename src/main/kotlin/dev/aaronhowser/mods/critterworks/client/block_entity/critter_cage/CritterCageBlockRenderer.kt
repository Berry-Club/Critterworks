package dev.aaronhowser.mods.critterworks.client.block_entity.critter_cage

import com.mojang.blaze3d.vertex.PoseStack
import dev.aaronhowser.mods.aaron.misc.AaronDsls.withPose
import dev.aaronhowser.mods.critterworks.block.CritterCageBlock
import dev.aaronhowser.mods.critterworks.block_entity.CritterCageBlockEntity
import dev.aaronhowser.mods.critterworks.entity.scoochworm.ScoochwormEntity
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider

class CritterCageBlockRenderer(
	context: BlockEntityRendererProvider.Context
) : BlockEntityRenderer<CritterCageBlockEntity> {

	override fun render(
		blockEntity: CritterCageBlockEntity,
		partialTick: Float,
		poseStack: PoseStack,
		bufferSource: MultiBufferSource,
		packedLight: Int,
		packedOverlay: Int
	) {
		val down = blockEntity.blockState.getValue(CritterCageBlock.DOWN)
		val forward = blockEntity.blockState.getValue(CritterCageBlock.FORWARD)

		val worm = blockEntity.getScoochworm() ?: return
		worm.supportDirection = down

		val yaw = ScoochwormEntity.getMovementYaw(forward, down)
		worm.yRot = yaw
		worm.yRotO = yaw
		worm.yBodyRot = yaw
		worm.yHeadRot = yaw
		worm.yHeadRotO = yaw

		poseStack.withPose {
			poseStack.translate(0.5, 0.05, 0.5)
			Minecraft.getInstance().entityRenderDispatcher.getRenderer(worm).render(
				worm,
				yaw,
				partialTick,
				poseStack,
				bufferSource,
				packedLight
			)
		}
	}

}