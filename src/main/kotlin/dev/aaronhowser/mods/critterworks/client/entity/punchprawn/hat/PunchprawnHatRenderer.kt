package dev.aaronhowser.mods.critterworks.client.entity.punchprawn.hat

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import dev.aaronhowser.mods.critterworks.Critterworks
import net.minecraft.client.renderer.MultiBufferSource
import software.bernie.geckolib.animatable.GeoAnimatable
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.animatable.instance.SingletonAnimatableInstanceCache
import software.bernie.geckolib.animation.AnimatableManager
import software.bernie.geckolib.cache.`object`.BakedGeoModel
import software.bernie.geckolib.model.DefaultedGeoModel
import software.bernie.geckolib.renderer.GeoObjectRenderer

class PunchprawnHatRenderer : GeoObjectRenderer<PunchprawnHatRenderer.HatAnimatable>(PunchprawnHatModel()) {

	fun renderHat(
		poseStack: PoseStack,
		bufferSource: MultiBufferSource,
		packedLight: Int,
		partialTick: Float
	) {
		val renderType = getRenderType(
			HatAnimatable,
			getTextureLocation(HatAnimatable),
			bufferSource,
			partialTick
		) ?: return

		val buffer = bufferSource.getBuffer(renderType)
		render(poseStack, HatAnimatable, bufferSource, renderType, buffer, packedLight, partialTick)
	}

	override fun getInstanceId(animatable: HatAnimatable): Long = 0

	override fun preRender(
		poseStack: PoseStack,
		animatable: HatAnimatable,
		model: BakedGeoModel,
		bufferSource: MultiBufferSource?,
		buffer: VertexConsumer?,
		isReRender: Boolean,
		partialTick: Float,
		packedLight: Int,
		packedOverlay: Int,
		colour: Int
	) {
	}

	object HatAnimatable : GeoAnimatable {
		private val animatableInstanceCache = SingletonAnimatableInstanceCache(this)

		override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar) {}
		override fun getAnimatableInstanceCache(): AnimatableInstanceCache = animatableInstanceCache
		override fun getTick(relatedObject: Any): Double = 0.0
	}

	class PunchprawnHatModel : DefaultedGeoModel<HatAnimatable>(
		Critterworks.modResource("punchprawn")
	) {
		override fun subtype(): String = "entity"
	}

}