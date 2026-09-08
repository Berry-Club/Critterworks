package dev.aaronhowser.mods.critterworks.client.render.entity.layer

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.math.Axis
import dev.aaronhowser.mods.aaron.misc.AaronDsls.withPose
import dev.aaronhowser.mods.critterworks.client.render.item.ScoochwormGpsItemRenderer
import dev.aaronhowser.mods.critterworks.entity.ScoochwormPartEntity
import dev.aaronhowser.mods.critterworks.registry.ModScoochwormAttachmentTypes
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.util.Mth
import software.bernie.geckolib.cache.`object`.BakedGeoModel
import software.bernie.geckolib.renderer.GeoRenderer
import software.bernie.geckolib.renderer.layer.GeoRenderLayer

class ScoochwormChunkLoaderLayer(renderer: GeoRenderer<ScoochwormPartEntity>) : GeoRenderLayer<ScoochwormPartEntity>(renderer) {
	override fun render(poseStack: PoseStack, animatable: ScoochwormPartEntity, bakedModel: BakedGeoModel, renderType: RenderType?, bufferSource: MultiBufferSource, buffer: VertexConsumer?, partialTick: Float, packedLight: Int, packedOverlay: Int) {
		if (animatable.attachmentData.resolveType() != ModScoochwormAttachmentTypes.CHUNK_LOADER.get()) return

		val animationTime = animatable.tickCount + partialTick
		val bob = Mth.sin(animationTime * BOB_SPEED) * BOB_HEIGHT

		poseStack.withPose {
			poseStack.translate(0.0, (ENTITY_DIAMOND_CENTER_Y + bob).toDouble(), 0.0)
			poseStack.mulPose(Axis.YP.rotationDegrees(animationTime * ROTATION_SPEED))
			poseStack.translate(0.0, (-DIAMOND_CENTER_Y).toDouble(), 0.0)

			ScoochwormGpsItemRenderer.renderShape(poseStack, bufferSource, packedLight, packedOverlay)
		}
	}

	companion object {
		private const val DIAMOND_HEIGHT = 6f / 16f
		private const val DIAMOND_CENTER_Y = DIAMOND_HEIGHT / 2f
		private const val ENTITY_DIAMOND_CENTER_Y = 20f / 16f
		private const val BOB_SPEED = 0.08f
		private const val BOB_HEIGHT = 0.5f / 16f
		private const val ROTATION_SPEED = 1.5f
	}
}