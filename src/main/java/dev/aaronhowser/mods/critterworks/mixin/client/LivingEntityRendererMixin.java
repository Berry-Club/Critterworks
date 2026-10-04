package dev.aaronhowser.mods.critterworks.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.aaronhowser.mods.critterworks.client.model.entity.ScoochwormModel;
import dev.aaronhowser.mods.critterworks.client.render.TintingMultiBufferSource;
import dev.aaronhowser.mods.critterworks.entity.ScoochwormPartEntity;
import dev.aaronhowser.mods.critterworks.entity.data.WormColor;
import dev.aaronhowser.mods.critterworks.registry.ModMobEffects;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
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

	@WrapMethod(
		method = "setupRotations(Lnet/minecraft/world/entity/LivingEntity;Lcom/mojang/blaze3d/vertex/PoseStack;FFFF)V"
	)
	private void critterworks$orientScoochwormPassenger(
		LivingEntity entity,
		PoseStack poseStack,
		float bob,
		float bodyRotation,
		float partialTick,
		float scale,
		Operation<Void> original
	) {
		if (entity.getVehicle() instanceof ScoochwormPartEntity scoochwormPart) {
			Vec3 actualAttachmentOffset = scoochwormPart
				.getPassengerRidingPosition(entity)
				.subtract(scoochwormPart.position());

			Vec3 renderOffset = scoochwormPart
				.getVisualPassengerAttachmentOffset()
				.subtract(actualAttachmentOffset)
				.scale(1.0 / scale);

			poseStack.translate(renderOffset.x, renderOffset.y, renderOffset.z);

			Vec3 passengerAttachmentPoint = entity
				.getVehicleAttachmentPoint(scoochwormPart)
				.scale(1.0 / scale);

			poseStack.translate(
				passengerAttachmentPoint.x,
				passengerAttachmentPoint.y,
				passengerAttachmentPoint.z
			);
			poseStack.mulPose(ScoochwormModel.getSurfaceRotation(scoochwormPart.getSupportDirection()));
			poseStack.translate(
				-passengerAttachmentPoint.x,
				-passengerAttachmentPoint.y,
				-passengerAttachmentPoint.z
			);
		}

		original.call(entity, poseStack, bob, bodyRotation, partialTick, scale);
	}
}