package dev.aaronhowser.mods.critterworks.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.aaronhowser.mods.critterworks.handler.web.redstone.RedstoneWebHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.SignalGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SignalGetter.class)
public interface SignalGetterMixin {

	@ModifyReturnValue(
		method = "getSignal",
		at = @At("RETURN")
	)
	default int critterworks$includeRedstoneWebSignal(int original, BlockPos position, Direction direction) {
		return critterworks$getModifiedSignal(original, position, direction);
	}

	@ModifyReturnValue(
		method = "getDirectSignal",
		at = @At("RETURN")
	)
	default int critterworks$includeDirectRedstoneWebSignal(int original, BlockPos position, Direction direction) {
		return critterworks$getModifiedSignal(original, position, direction);
	}

	@Unique
	private int critterworks$getModifiedSignal(int original, BlockPos position, Direction direction) {
		if (original >= 15) return original;
		if (!((Object) this instanceof ServerLevel serverLevel)) return original;

		int webSignal = RedstoneWebHandler.getInjectedPower(serverLevel, position, direction);
		return Math.max(original, webSignal);
	}
}