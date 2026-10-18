package dev.aaronhowser.mods.critterworks.client.item.lockbox

import com.mojang.blaze3d.vertex.PoseStack
import dev.aaronhowser.mods.aaron.misc.AaronDsls.withPose
import dev.aaronhowser.mods.critterworks.item.LockboxItem
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import software.bernie.geckolib.renderer.GeoItemRenderer

class LockboxItemRenderer : GeoItemRenderer<LockboxItem>(LockboxItemModel()) {

	override fun renderByItem(
		stack: ItemStack,
		displayContext: ItemDisplayContext,
		poseStack: PoseStack,
		bufferSource: MultiBufferSource,
		packedLight: Int,
		packedOverlay: Int
	) {
		poseStack.withPose {
			poseStack.translate(0.0, -MODEL_Y_OFFSET, 0.0)

			super.renderByItem(
				stack,
				displayContext,
				poseStack,
				bufferSource,
				packedLight,
				packedOverlay
			)
		}
	}

	companion object {
		private const val MODEL_Y_OFFSET = 17.25 / 16.0
	}

}