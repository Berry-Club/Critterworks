package dev.aaronhowser.mods.critterworks.client.entity.punchprawn

import com.mojang.blaze3d.vertex.PoseStack
import dev.aaronhowser.mods.critterworks.entity.punchprawn.PunchprawnEntity
import net.minecraft.client.renderer.entity.EntityRendererProvider
import software.bernie.geckolib.cache.`object`.BakedGeoModel
import software.bernie.geckolib.renderer.GeoEntityRenderer

class PunchprawnRenderer(
	context: EntityRendererProvider.Context
) : GeoEntityRenderer<PunchprawnEntity>(context, PunchprawnModel()) {

	override fun scaleModelForRender(
		widthScale: Float,
		heightScale: Float,
		poseStack: PoseStack,
		animatable: PunchprawnEntity,
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