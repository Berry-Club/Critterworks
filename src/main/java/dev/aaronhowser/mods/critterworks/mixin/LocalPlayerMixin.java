package dev.aaronhowser.mods.critterworks.mixin;

import dev.aaronhowser.mods.critterworks.entity.filterfeeder.FilterfeederEntity;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {

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