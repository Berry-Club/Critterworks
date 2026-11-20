package dev.aaronhowser.mods.critterworks.client.entity.parry_platypus

import com.mojang.blaze3d.vertex.PoseStack
import dev.aaronhowser.mods.critterworks.entity.parry_platypus.ParryPlatypusEntity
import net.minecraft.client.renderer.entity.EntityRendererProvider
import software.bernie.geckolib.cache.`object`.BakedGeoModel
import software.bernie.geckolib.renderer.GeoEntityRenderer

class ParryPlatypusRenderer(
	context: EntityRendererProvider.Context
) : GeoEntityRenderer<ParryPlatypusEntity>(context, ParryPlatypusModel()) {

	override fun scaleModelForRender(
		widthScale: Float,
		heightScale: Float,
		poseStack: PoseStack,
		animatable: ParryPlatypusEntity,
		model: BakedGeoModel,
		isReRender: Boolean,
		partialTick: Float,
		packedLight: Int,
		packedOverlay: Int
	) {
		if (!isReRender && animatable.isBaby) {
			poseStack.scale(0.5f, 0.5f, 0.5f)
		}
	}

}