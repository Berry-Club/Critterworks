package dev.aaronhowser.mods.critterworks.client.entity.scoochworm.attachment

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import dev.aaronhowser.mods.aaron.misc.AaronDsls.withPose
import dev.aaronhowser.mods.critterworks.entity.scoochworm.ScoochwormPartEntity
import dev.aaronhowser.mods.critterworks.entity.scoochworm.attachment.ScoochwormAttachmentType
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import software.bernie.geckolib.cache.`object`.BakedGeoModel
import software.bernie.geckolib.model.GeoModel
import software.bernie.geckolib.renderer.GeoRenderer
import software.bernie.geckolib.renderer.layer.GeoRenderLayer

abstract class ScoochwormPartAttachmentLayer(
	renderer: GeoRenderer<ScoochwormPartEntity>,
	private val attachmentType: ScoochwormAttachmentType<*>,
	private val attachmentModel: GeoModel<ScoochwormPartEntity>
) : GeoRenderLayer<ScoochwormPartEntity>(renderer) {

	override fun getGeoModel(): GeoModel<ScoochwormPartEntity> = attachmentModel

	protected open fun prepareModel(
		animatable: ScoochwormPartEntity,
		partialTick: Float
	) {
	}

	override fun render(
		poseStack: PoseStack,
		animatable: ScoochwormPartEntity,
		bakedModel: BakedGeoModel,
		renderType: RenderType?,
		bufferSource: MultiBufferSource,
		buffer: VertexConsumer?,
		partialTick: Float,
		packedLight: Int,
		packedOverlay: Int
	) {
		if (animatable.attachmentData.resolveType() != attachmentType) return

		val texture = attachmentModel.getTextureResource(animatable, renderer)
		val attachmentRenderType = renderer.getRenderType(
			animatable,
			texture,
			bufferSource,
			partialTick
		) ?: return
		val attachmentBakedModel = getDefaultBakedModel(animatable)
		prepareModel(animatable, partialTick)

		poseStack.withPose {
			renderer.reRender(
				attachmentBakedModel,
				poseStack,
				bufferSource,
				animatable,
				attachmentRenderType,
				bufferSource.getBuffer(attachmentRenderType),
				partialTick,
				packedLight,
				packedOverlay,
				renderer.getRenderColor(animatable, partialTick, packedLight).argbInt()
			)
		}
	}
}