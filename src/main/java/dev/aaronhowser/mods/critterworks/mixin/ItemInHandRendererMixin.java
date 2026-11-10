package dev.aaronhowser.mods.critterworks.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.aaronhowser.mods.critterworks.client.render.TintingMultiBufferSource;
import dev.aaronhowser.mods.critterworks.entity.scoochworm.data.WormColor;
import dev.aaronhowser.mods.critterworks.registry.ModItems;
import dev.aaronhowser.mods.critterworks.registry.ModMobEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {

	@Inject(
		method = "itemUsed",
		at = @At("HEAD"),
		cancellable = true
	)
	private void critterworks$keepParryPlatypusRaised(InteractionHand hand, CallbackInfo callbackInfo) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null || !player.isUsingItem() || player.getUsedItemHand() != hand) return;
		if (player.getUseItem().getItem() != ModItems.INSTANCE.getPARRY_PLATYPUS().get()) return;

		callbackInfo.cancel();
	}

	@WrapMethod(method = "renderPlayerArm")
	private void critterworks$tintDyedPlayerArm(
		PoseStack poseStack,
		MultiBufferSource bufferSource,
		int packedLight,
		float equippedProgress,
		float swingProgress,
		HumanoidArm side,
		Operation<Void> original
	) {
		LocalPlayer player = Minecraft.getInstance().player;

		if (player != null) {
			WormColor wormColor = ModMobEffects.INSTANCE.getDyeColor(player);

			if (wormColor != null) {
				bufferSource = new TintingMultiBufferSource(
					bufferSource,
					wormColor.getTintColor()
				);
			}
		}

		original.call(
			poseStack,
			bufferSource,
			packedLight,
			equippedProgress,
			swingProgress,
			side
		);
	}
}