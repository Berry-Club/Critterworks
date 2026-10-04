package dev.aaronhowser.mods.critterworks.mixin.client;

import dev.aaronhowser.mods.critterworks.client.model.entity.ScoochwormModel;
import dev.aaronhowser.mods.critterworks.config.ClientConfig;
import dev.aaronhowser.mods.critterworks.entity.ScoochwormPartEntity;
import net.minecraft.client.Camera;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {

	@Shadow
	private float eyeHeight;

	@Shadow
	private float eyeHeightOld;

	@Shadow
	protected abstract void setPosition(Vec3 position);

	@Inject(
		method = "setup(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;ZZF)V",
		at = @At("RETURN")
	)
	private void critterworks$positionScoochwormPassengerCamera(
		BlockGetter level,
		Entity entity,
		boolean detached,
		boolean thirdPersonReverse,
		float partialTick,
		CallbackInfo callbackInfo
	) {
		if (detached || !(entity.getVehicle() instanceof ScoochwormPartEntity scoochwormPart)) {
			return;
		}

		double interpolatedEyeHeight = Mth.lerp(partialTick, eyeHeightOld, eyeHeight);
		Vec3 entityPosition = new Vec3(
			Mth.lerp(partialTick, entity.xo, entity.getX()),
			Mth.lerp(partialTick, entity.yo, entity.getY()),
			Mth.lerp(partialTick, entity.zo, entity.getZ())
		);

		Vec3 actualAttachmentOffset = scoochwormPart
			.getPassengerRidingPosition(entity)
			.subtract(scoochwormPart.position());

		Vec3 renderOffset = scoochwormPart
			.getVisualPassengerAttachmentOffset()
			.subtract(actualAttachmentOffset);

		Vec3 passengerAttachmentPoint = entity.getVehicleAttachmentPoint(scoochwormPart);

		Vector3f eyeOffsetFromAttachment = new Vector3f(
			(float) -passengerAttachmentPoint.x,
			(float) (interpolatedEyeHeight - passengerAttachmentPoint.y),
			(float) -passengerAttachmentPoint.z
		);

		Quaternionf surfaceRotation = ScoochwormModel.getSurfaceRotation(scoochwormPart.getSupportDirection());
		surfaceRotation.transform(eyeOffsetFromAttachment);

		Vec3 cameraPosition = entityPosition
			.add(renderOffset)
			.add(passengerAttachmentPoint)
			.add(new Vec3(eyeOffsetFromAttachment));

		setPosition(cameraPosition);

		if (ClientConfig.CONFIG.rotateScoochwormPassengerCamera.get()) {
			Camera camera = (Camera) (Object) this;

			surfaceRotation.transform(camera.getLookVector());
			surfaceRotation.transform(camera.getUpVector());
			surfaceRotation.transform(camera.getLeftVector());
			surfaceRotation.mul(camera.rotation(), camera.rotation());
		}
	}

}