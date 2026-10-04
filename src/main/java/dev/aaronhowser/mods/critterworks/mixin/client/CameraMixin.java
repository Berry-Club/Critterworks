package dev.aaronhowser.mods.critterworks.mixin.client;

import dev.aaronhowser.mods.critterworks.client.model.entity.ScoochwormModel;
import dev.aaronhowser.mods.critterworks.entity.ScoochwormPartEntity;
import net.minecraft.client.Camera;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {

	@Shadow
	protected abstract void setPosition(Vec3 position);

	@Inject(
		method = "setup(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;ZZF)V",
		at = @At("RETURN")
	)
	private void critterworks$orientScoochwormPassenger(
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

		Direction supportDirection = scoochwormPart.getSupportDirection();

		Quaternionf surfaceRotation = ScoochwormModel.getSurfaceRotation(supportDirection);
		Camera camera = (Camera) (Object) this;

		double entityY = Mth.lerp(partialTick, entity.yo, entity.getY());
		double eyeHeight = camera.getPosition().y - entityY;

		Vec3 segmentPosition = new Vec3(
			Mth.lerp(partialTick, scoochwormPart.xo, scoochwormPart.getX()),
			Mth.lerp(partialTick, scoochwormPart.yo, scoochwormPart.getY()),
			Mth.lerp(partialTick, scoochwormPart.zo, scoochwormPart.getZ())
		);

		Vec3 topNormal = Vec3.atLowerCornerOf(supportDirection.getNormal()).reverse();
		Vec3 passengerAttachmentPosition = segmentPosition.add(scoochwormPart.getTopFaceCenterOffset());

		setPosition(passengerAttachmentPosition.add(topNormal.scale(eyeHeight)));

		surfaceRotation.transform(camera.getLookVector());
		surfaceRotation.transform(camera.getUpVector());
		surfaceRotation.transform(camera.getLeftVector());
		surfaceRotation.mul(camera.rotation(), camera.rotation());
	}

}