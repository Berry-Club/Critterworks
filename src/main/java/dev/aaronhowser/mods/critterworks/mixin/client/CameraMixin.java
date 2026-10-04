package dev.aaronhowser.mods.critterworks.mixin.client;

import dev.aaronhowser.mods.critterworks.client.model.entity.ScoochwormModel;
import dev.aaronhowser.mods.critterworks.config.ClientConfig;
import dev.aaronhowser.mods.critterworks.entity.ScoochwormPartEntity;
import net.minecraft.client.Camera;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {

	@Unique
	private int critterworks$previousScoochwormPartId = -1;

	@Unique
	private Direction critterworks$previousSupportDirection;

	@Shadow
	private float eyeHeight;

	@Shadow
	private float eyeHeightOld;

	@Shadow
	protected abstract void setPosition(Vec3 position);

	@Inject(
		method = "setup(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;ZZF)V",
		at = @At("HEAD")
	)
	private void critterworks$preserveScoochwormPassengerLookDirection(
		BlockGetter level,
		Entity entity,
		boolean detached,
		boolean thirdPersonReverse,
		float partialTick,
		CallbackInfo callbackInfo
	) {
		if (!(entity.getVehicle() instanceof ScoochwormPartEntity scoochwormPart)) {
			critterworks$previousScoochwormPartId = -1;
			critterworks$previousSupportDirection = null;
			return;
		}

		Direction supportDirection = scoochwormPart.getSupportDirection();
		boolean sameScoochwormPart = critterworks$previousScoochwormPartId == scoochwormPart.getId();

		if (
			sameScoochwormPart
				&& critterworks$previousSupportDirection != null
				&& critterworks$previousSupportDirection != supportDirection
				&& ClientConfig.CONFIG.rotateScoochwormPassengerCamera.get()
		) {
			critterworks$preserveLookDirection(
				entity,
				critterworks$previousSupportDirection,
				supportDirection
			);
		}

		critterworks$previousScoochwormPartId = scoochwormPart.getId();
		critterworks$previousSupportDirection = supportDirection;
	}

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

	@Unique
	private static void critterworks$preserveLookDirection(
		Entity entity,
		Direction previousSupportDirection,
		Direction supportDirection
	) {
		Vector3f worldLookDirection = Vec3
			.directionFromRotation(entity.getXRot(), entity.getYRot())
			.toVector3f();

		ScoochwormModel.getSurfaceRotation(previousSupportDirection)
			.transform(worldLookDirection);

		Vector3f localLookDirection = ScoochwormModel
			.getSurfaceRotation(supportDirection)
			.conjugate()
			.transform(worldLookDirection);

		float horizontalDistance = (float) Math.sqrt(
			localLookDirection.x * localLookDirection.x
				+ localLookDirection.z * localLookDirection.z
		);

		float pitch = (float) Math.toDegrees(Math.atan2(-localLookDirection.y, horizontalDistance));
		float yaw = entity.getYRot();

		if (horizontalDistance > 0.000001f) {
			yaw = (float) Math.toDegrees(Math.atan2(-localLookDirection.x, localLookDirection.z));
		}

		entity.setXRot(pitch);
		entity.setYRot(yaw);
		entity.xRotO = pitch;
		entity.yRotO = yaw;

		if (entity instanceof LivingEntity livingEntity) {
			livingEntity.yHeadRot = yaw;
			livingEntity.yHeadRotO = yaw;
		}
	}

}