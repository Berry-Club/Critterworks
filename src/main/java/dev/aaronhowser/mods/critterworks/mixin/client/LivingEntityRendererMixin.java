package dev.aaronhowser.mods.critterworks.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.aaronhowser.mods.critterworks.client.render.TintingMultiBufferSource;
import dev.aaronhowser.mods.critterworks.entity.data.WormColor;
import dev.aaronhowser.mods.critterworks.registry.ModMobEffects;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {

	@WrapMethod(
		method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"
	)
	private void critterworks$tintDyedEntity(
		LivingEntity entity,
		float entityYaw,
		float partialTick,
		PoseStack poseStack,
		MultiBufferSource bufferSource,
		int packedLight,
		Operation<Void> original
	) {
		WormColor wormColor = ModMobEffects.INSTANCE.getDyeColor(entity);

		if (wormColor != null) {
			bufferSource = new TintingMultiBufferSource(
				bufferSource,
				wormColor.getTintColor()
			);
		}

		original.call(
			entity,
			entityYaw,
			partialTick,
			poseStack,
			bufferSource,
			packedLight
		);
	}
}