package dev.aaronhowser.mods.critterworks.client.entity.sproutsnail

import com.mojang.blaze3d.vertex.PoseStack
import dev.aaronhowser.mods.critterworks.entity.sproutsnail.SproutsnailEntity
import net.minecraft.client.renderer.entity.EntityRendererProvider
import software.bernie.geckolib.cache.`object`.BakedGeoModel
import software.bernie.geckolib.renderer.GeoEntityRenderer

//TODO: Walk animation where it stretches forward etc
class SproutsnailRenderer(
	context: EntityRendererProvider.Context
) : GeoEntityRenderer<SproutsnailEntity>(context, SproutsnailModel()) {

	init {
		addRenderLayer(SproutsnailFlowerLayer(this))
	}

	override fun scaleModelForRender(
		widthScale: Float,
		heightScale: Float,
		poseStack: PoseStack,
		animatable: SproutsnailEntity,
		model: BakedGeoModel,
		isReRender: Boolean,
		partialTick: Float,
		packedLight: Int,
		packedOverlay: Int
	) {
		var scaledWidth = widthScale
		var scaledHeight = heightScale

		if (animatable.isBaby) {
			scaledWidth *= 0.5f
			scaledHeight *= 0.5f
		}

		super.scaleModelForRender(
			scaledWidth,
			scaledHeight,
			poseStack,
			animatable,
			model,
			isReRender,
			partialTick,
			packedLight,
			packedOverlay
		)
	}

}