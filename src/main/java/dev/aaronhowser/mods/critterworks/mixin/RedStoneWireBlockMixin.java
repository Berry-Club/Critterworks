package dev.aaronhowser.mods.critterworks.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.aaronhowser.mods.critterworks.handler.web.redstone.RedstoneWebHandler;
import net.minecraft.world.level.block.RedStoneWireBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(RedStoneWireBlock.class)
public class RedStoneWireBlockMixin {

	@ModifyReturnValue(
		method = "getSignal",
		at = @At("RETURN")
	)
	private int critterworks$suppressSignalWhileReadingWebstoneInput(int original) {
		if (RedstoneWebHandler.isWebstonePowerSuppressed()) return 0;
		return original;
	}
}