package dev.aaronhowser.mods.critterworks.client.block_entity.critter_cage

import com.mojang.blaze3d.vertex.PoseStack
import dev.aaronhowser.mods.critterworks.block.CritterCageBlock
import dev.aaronhowser.mods.critterworks.block_entity.CritterCageBlockEntity
import dev.aaronhowser.mods.critterworks.client.critter_cage.CritterCageEntityRenderer
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
		val entity = blockEntity.getStoredEntity() ?: return

		CritterCageEntityRenderer.render(
			entity,
			down,
			forward,
			partialTick,
			poseStack,
			bufferSource,
			packedLight
		)
	}

}