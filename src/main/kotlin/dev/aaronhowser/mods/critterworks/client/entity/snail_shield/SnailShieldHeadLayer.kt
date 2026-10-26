package dev.aaronhowser.mods.critterworks.client.entity.snail_shield

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import dev.aaronhowser.mods.aaron.misc.AaronDsls.withPose
import dev.aaronhowser.mods.critterworks.item.SnailShieldItem.SnailShieldState
import dev.aaronhowser.mods.critterworks.registry.ModAttachmentTypes
import net.minecraft.client.model.SkullModel
import net.minecraft.client.model.geom.ModelLayers
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.util.Mth
import software.bernie.geckolib.cache.`object`.GeoBone
import software.bernie.geckolib.renderer.layer.GeoRenderLayer
import software.bernie.geckolib.util.RenderUtil

class SnailShieldHeadLayer(
	private val shellRenderer: SnailShieldRenderer,
	context: EntityRendererProvider.Context
) : GeoRenderLayer<SnailShieldRenderer.ShellAnimatable>(shellRenderer) {

	private val headModel = SkullModel(context.bakeLayer(ModelLayers.PLAYER_HEAD))

	override fun renderForBone(
		poseStack: PoseStack,
		animatable: SnailShieldRenderer.ShellAnimatable,
		bone: GeoBone,
		renderType: RenderType,
		bufferSource: MultiBufferSource,
		buffer: VertexConsumer,
		partialTick: Float,
		packedLight: Int,
		packedOverlay: Int
	) {
		if (bone.name != SHELL_BONE_NAME) return

		val player = shellRenderer.currentEntity as? AbstractClientPlayer ?: return
		val shellState = player.getData(ModAttachmentTypes.SNAIL_SHIELD_STATE)
		if (shellState != SnailShieldState.PARTIALLY_SHELLED) return

		poseStack.withPose {
			RenderUtil.translateToPivotPoint(poseStack, bone)
			poseStack.scale(-1f, -1f, 1f)

			headModel.setupAnim(
				0f,
				getRelativeHeadYaw(player, partialTick),
				player.getViewXRot(partialTick)
			)

			val headRenderType = RenderType.entityTranslucent(player.skin.texture())
			val headBuffer = bufferSource.getBuffer(headRenderType)

			headModel.renderToBuffer(
				poseStack,
				headBuffer,
				packedLight,
				OverlayTexture.NO_OVERLAY
			)

			bufferSource.getBuffer(renderType)
		}
	}

	private fun getRelativeHeadYaw(player: AbstractClientPlayer, partialTick: Float): Float {
		val bodyYaw = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot)
		val headYaw = Mth.rotLerp(partialTick, player.yHeadRotO, player.yHeadRot)

		return Mth.wrapDegrees(headYaw - bodyYaw)
	}

	companion object {
		private const val SHELL_BONE_NAME = "head_pos"
	}
}