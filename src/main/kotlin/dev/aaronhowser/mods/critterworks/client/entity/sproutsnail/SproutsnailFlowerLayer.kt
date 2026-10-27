package dev.aaronhowser.mods.critterworks.client.entity.sproutsnail

import com.mojang.blaze3d.vertex.PoseStack
import dev.aaronhowser.mods.critterworks.entity.sproutsnail.SproutsnailEntity
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import software.bernie.geckolib.cache.`object`.GeoBone
import software.bernie.geckolib.renderer.GeoRenderer
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer

//TODO: Figure out why lighting is weird
class SproutsnailFlowerLayer(
	renderer: GeoRenderer<SproutsnailEntity>
) : BlockAndItemGeoLayer<SproutsnailEntity>(renderer) {

	override fun getBlockForBone(
		bone: GeoBone,
		animatable: SproutsnailEntity
	): BlockState? {
		if (bone.name != FLOWER_BONE_NAME) return null

		val flowerBlock = Block.byItem(animatable.flower.item)
		if (flowerBlock === Blocks.AIR) return null

		return flowerBlock.defaultBlockState()
	}

	override fun renderBlockForBone(
		poseStack: PoseStack,
		bone: GeoBone,
		state: BlockState,
		animatable: SproutsnailEntity,
		bufferSource: MultiBufferSource,
		partialTick: Float,
		packedLight: Int,
		packedOverlay: Int
	) {
		poseStack.pushPose()
		poseStack.scale(FLOWER_SCALE, FLOWER_SCALE, FLOWER_SCALE)
		poseStack.translate(-0.5f, 0f, -0.5f)

		Minecraft.getInstance()
			.blockRenderer
			.renderSingleBlock(
				state,
				poseStack,
				bufferSource,
				packedLight,
				OverlayTexture.NO_OVERLAY
			)

		poseStack.popPose()
	}

	companion object {
		private const val FLOWER_BONE_NAME = "flower_pos"
		private const val FLOWER_SCALE = 0.5f
	}

}