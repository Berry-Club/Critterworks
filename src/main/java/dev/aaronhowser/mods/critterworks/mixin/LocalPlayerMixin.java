package dev.aaronhowser.mods.critterworks.mixin;

import dev.aaronhowser.mods.critterworks.block_entity.ItemFilterfeederBlockEntity;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {

	@Inject(
		method = "getWaterVision",
		at = @At("HEAD"),
		cancellable = true
	)
	private void critterworks$clamFog(CallbackInfoReturnable<Float> cir) {
		var player = (LocalPlayer) (Object) this;
		var isInRange = ItemFilterfeederBlockEntity.isWithinRange(player.level(), player.blockPosition());

		if (isInRange) {
			cir.setReturnValue(1f);
		}
	}

}