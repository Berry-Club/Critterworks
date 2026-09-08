package dev.aaronhowser.mods.critterworks.client.render.entity

import com.mojang.blaze3d.vertex.PoseStack
import dev.aaronhowser.mods.critterworks.client.model.entity.ScoochwormPartModel
import dev.aaronhowser.mods.critterworks.client.render.entity.layer.ScoochwormChunkLoaderLayer
import dev.aaronhowser.mods.critterworks.client.render.entity.layer.ScoochwormLockboxLayer
import dev.aaronhowser.mods.critterworks.client.render.entity.layer.ScoochwormSaddleLayer
import dev.aaronhowser.mods.critterworks.entity.ScoochwormEntity
import dev.aaronhowser.mods.critterworks.entity.ScoochwormPartEntity
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.Mth
import software.bernie.geckolib.renderer.GeoEntityRenderer

class ScoochwormPartRenderer(
	context: EntityRendererProvider.Context
) : GeoEntityRenderer<ScoochwormPartEntity>(context, ScoochwormPartModel()) {

	init {
		withScale(ScoochwormEntity.SIZE)
		addRenderLayer(ScoochwormChunkLoaderLayer(this))
		addRenderLayer(ScoochwormLockboxLayer(this))
		addRenderLayer(ScoochwormSaddleLayer(this))
	}

	override fun getTextureLocation(animatable: ScoochwormPartEntity): ResourceLocation {
		return animatable.color.bodyTexture
	}

	override fun applyRotations(
		animatable: ScoochwormPartEntity,
		poseStack: PoseStack,
		ageInTicks: Float,
		rotationYaw: Float,
		partialTick: Float,
		nativeScale: Float
	) {
		val interpolatedYaw = Mth.rotLerp(
			partialTick,
			animatable.yRotO,
			animatable.yRot
		)

		ScoochwormRenderer.applyRotations(
			poseStack,
			animatable.supportDirection,
			interpolatedYaw
		)

		val partScale = getPartScale(animatable.partIndex)
		poseStack.scale(partScale, partScale, partScale)
	}

	private fun getPartScale(partIndex: Int): Float {
		return 1f - PART_SCALE_DECREMENT * (partIndex + 1)
	}

	companion object {
		private const val PART_SCALE_DECREMENT = 0.001f
	}
}