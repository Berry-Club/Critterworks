package dev.aaronhowser.mods.critterworks.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.aaronhowser.mods.critterworks.effect.AaronMobEffect;
import dev.aaronhowser.mods.critterworks.registry.ModMobEffects;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin {

	@WrapMethod(method = "getSkin")
	private PlayerSkin critterworks$useAaronSkin(Operation<PlayerSkin> original) {
		PlayerSkin originalSkin = original.call();
		AbstractClientPlayer player = (AbstractClientPlayer) (Object) this;

		if (!player.hasEffect(ModMobEffects.INSTANCE.getAARON())) {
			return originalSkin;
		}

		return new PlayerSkin(
			AaronMobEffect.SKIN,
			null,
			originalSkin.capeTexture(),
			originalSkin.elytraTexture(),
			PlayerSkin.Model.WIDE,
			true
		);
	}
}