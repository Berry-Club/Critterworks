package dev.aaronhowser.mods.critterworks.mixin;

import dev.aaronhowser.mods.critterworks.entity.filterfeeder.FilterfeederEntity;
import dev.aaronhowser.mods.critterworks.registry.ModAttachmentTypes;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {

	@ModifyVariable(
		method = "move",
		at = @At("HEAD"),
		argsOnly = true
	)
	private Vec3 critterworks$preventMovementWhileShelled(Vec3 movement) {
		var player = (LocalPlayer) (Object) this;
		if (player.getData(ModAttachmentTypes.IS_SHELL_ACTIVE)) {
			return Vec3.ZERO;
		}

		return movement;
	}

	@Inject(
		method = "getWaterVision",
		at = @At("RETURN"),
		cancellable = true
	)
	private void critterworks$filterfeederFog(CallbackInfoReturnable<Float> cir) {
		var player = (LocalPlayer) (Object) this;
		var minimumWaterVision = FilterfeederEntity.getMinWaterFog(player.level(), player.position());

		if (minimumWaterVision > cir.getReturnValue()) {
			cir.setReturnValue(minimumWaterVision);
		}
	}

}
