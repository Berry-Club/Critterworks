package dev.aaronhowser.mods.critterworks.client.entity.evil_snail

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.math.Axis
import dev.aaronhowser.mods.aaron.misc.AaronDsls.withPose
import dev.aaronhowser.mods.critterworks.Critterworks
import dev.aaronhowser.mods.critterworks.handler.snail.ClientEvilSnails
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.LevelRenderer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.RenderLevelStageEvent
import software.bernie.geckolib.animatable.GeoAnimatable
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.animation.AnimatableManager
import software.bernie.geckolib.cache.`object`.BakedGeoModel
import software.bernie.geckolib.renderer.GeoObjectRenderer
import software.bernie.geckolib.util.GeckoLibUtil

@EventBusSubscriber(
	modid = Critterworks.MOD_ID,
	value = [Dist.CLIENT]
)
object EvilSnailRenderer : GeoObjectRenderer<EvilSnailRenderer.EvilSnailAnimatable>(EvilSnailModel()) {

	@SubscribeEvent
	fun renderSnails(event: RenderLevelStageEvent) {
		if (event.stage != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return

		val minecraft = Minecraft.getInstance()
		val level = minecraft.level ?: return
		val bufferSource = minecraft.renderBuffers().bufferSource()
		val cameraPosition = event.camera.position
		val partialTick = event.partialTick.getGameTimeDeltaPartialTick(true)
		val renderTimeNanos = System.nanoTime()

		for (snail in ClientEvilSnails.getSnails()) {
			val position = snail.getPosition(renderTimeNanos)
			val packedLight = LevelRenderer.getLightColor(level, BlockPos.containing(position))

			renderSnail(
				event.poseStack,
				bufferSource,
				cameraPosition,
				position,
				snail.getYRot(renderTimeNanos),
				packedLight,
				partialTick
			)
		}
	}

	private fun renderSnail(
		poseStack: PoseStack,
		bufferSource: MultiBufferSource,
		cameraPosition: Vec3,
		position: Vec3,
		yRot: Float,
		packedLight: Int,
		partialTick: Float
	) {
		val renderType = getRenderType(
			EvilSnailAnimatable,
			getTextureLocation(EvilSnailAnimatable),
			bufferSource,
			partialTick
		) ?: return
		val buffer = bufferSource.getBuffer(renderType)
		val renderPosition = position.subtract(cameraPosition)

		poseStack.withPose {
			poseStack.translate(renderPosition.x, renderPosition.y, renderPosition.z)
			poseStack.mulPose(Axis.YP.rotationDegrees(180f - yRot))

			render(
				poseStack,
				EvilSnailAnimatable,
				bufferSource,
				renderType,
				buffer,
				packedLight,
				partialTick
			)
		}
	}

	override fun getInstanceId(animatable: EvilSnailAnimatable): Long = 0

	override fun preRender(
		poseStack: PoseStack,
		animatable: EvilSnailAnimatable,
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

	object EvilSnailAnimatable : GeoAnimatable {
		private val animatableInstanceCache = GeckoLibUtil.createInstanceCache(this)

		override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar) {}
		override fun getAnimatableInstanceCache(): AnimatableInstanceCache = animatableInstanceCache
		override fun getTick(relatedObject: Any): Double = 0.0
	}

}