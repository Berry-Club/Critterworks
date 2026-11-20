package dev.aaronhowser.mods.critterworks.client.entity.parry_platypus

import com.mojang.blaze3d.vertex.PoseStack
import dev.aaronhowser.mods.critterworks.entity.parry_platypus.ParryPlatypusEntity
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.core.BlockPos
import software.bernie.geckolib.cache.`object`.BakedGeoModel
import software.bernie.geckolib.renderer.GeoEntityRenderer

class ParryPlatypusRenderer(
	context: EntityRendererProvider.Context
) : GeoEntityRenderer<ParryPlatypusEntity>(context, ParryPlatypusModel()) {

	override fun getBlockLightLevel(
		entity: ParryPlatypusEntity,
		blockPosition: BlockPos
	): Int {
		return maxOf(MINIMUM_LIGHT_LEVEL, super.getBlockLightLevel(entity, blockPosition))
	}

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
		var w = widthScale
		var h = heightScale

		if (animatable.isBaby) {
			w *= 0.5f
			h *= 0.5f
		}

		super.scaleModelForRender(w, h, poseStack, animatable, model, isReRender, partialTick, packedLight, packedOverlay)
	}

	companion object {
		private const val MINIMUM_LIGHT_LEVEL = 5
	}

}