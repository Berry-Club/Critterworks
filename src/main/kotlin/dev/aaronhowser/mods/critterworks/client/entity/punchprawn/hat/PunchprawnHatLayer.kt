package dev.aaronhowser.mods.critterworks.client.entity.punchprawn.hat

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import dev.aaronhowser.mods.aaron.misc.AaronDsls.withPose
import dev.aaronhowser.mods.critterworks.registry.ModAttachmentTypes
import net.minecraft.client.model.PlayerModel
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.entity.RenderLayerParent
import net.minecraft.client.renderer.entity.layers.RenderLayer

class PunchprawnHatLayer(
	parentRenderer: RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>
) : RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>(parentRenderer) {

	private val punchprawnRenderer = PunchprawnHatRenderer()

	override fun render(
		poseStack: PoseStack,
		bufferSource: MultiBufferSource,
		packedLight: Int,
		player: AbstractClientPlayer,
		limbSwing: Float,
		limbSwingAmount: Float,
		partialTick: Float,
		ageInTicks: Float,
		netHeadYaw: Float,
		headPitch: Float
	) {
		if (player.getData(ModAttachmentTypes.WORN_PUNCHPRAWN).isEmpty) return
		if (player.isInvisible) return

		poseStack.withPose {
			parentModel.head.translateAndRotate(poseStack)
			poseStack.translate(0.0, -0.45, 0.0)
			poseStack.mulPose(Axis.ZP.rotationDegrees(180f))
			punchprawnRenderer.renderHat(poseStack, bufferSource, packedLight, partialTick)

		}
	}

}