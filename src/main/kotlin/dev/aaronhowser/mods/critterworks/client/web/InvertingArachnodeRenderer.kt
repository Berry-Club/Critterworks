package dev.aaronhowser.mods.critterworks.client.web

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import dev.aaronhowser.mods.critterworks.Critterworks
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.resources.ResourceLocation
import software.bernie.geckolib.animatable.GeoAnimatable
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.animation.AnimatableManager
import software.bernie.geckolib.cache.`object`.BakedGeoModel
import software.bernie.geckolib.model.DefaultedGeoModel
import software.bernie.geckolib.renderer.GeoObjectRenderer
import software.bernie.geckolib.util.GeckoLibUtil

object InvertingArachnodeRenderer :
	GeoObjectRenderer<InvertingArachnodeRenderer.RenderState>(InvertingArachnodeModel()) {

	private val unpoweredState = RenderState(false)
	private val poweredState = RenderState(true)

	fun render(
		poseStack: PoseStack,
		bufferSource: MultiBufferSource,
		packedLight: Int,
		powered: Boolean
	) {
		val renderState = if (powered) poweredState else unpoweredState
		val renderType = getRenderType(
			renderState,
			getTextureLocation(renderState),
			bufferSource,
			0f
		) ?: return

		val buffer = bufferSource.getBuffer(renderType)
		poseStack.scale(WORLD_SCALE, WORLD_SCALE, WORLD_SCALE)
		render(poseStack, renderState, bufferSource, renderType, buffer, packedLight, 0f)
	}

	override fun getInstanceId(animatable: RenderState): Long {
		return if (animatable.powered) 1 else 0
	}

	override fun preRender(
		poseStack: PoseStack,
		animatable: RenderState,
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

	class RenderState(val powered: Boolean) : GeoAnimatable {
		private val animatableInstanceCache = GeckoLibUtil.createInstanceCache(this)

		override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar) = Unit
		override fun getAnimatableInstanceCache(): AnimatableInstanceCache = animatableInstanceCache
		override fun getTick(relatedObject: Any): Double = 0.0
	}

	class InvertingArachnodeModel : DefaultedGeoModel<RenderState>(
		Critterworks.modResource("inverting_arachnode")
	) {
		override fun subtype(): String = "item"

		override fun getTextureResource(animatable: RenderState): ResourceLocation {
			if (animatable.powered) return POWERED_TEXTURE

			return super.getTextureResource(animatable)
		}
	}

	private val POWERED_TEXTURE =
		Critterworks.modResource("textures/item/inverting_arachnode_powered.png")

	private const val WORLD_SCALE = 0.5f
}