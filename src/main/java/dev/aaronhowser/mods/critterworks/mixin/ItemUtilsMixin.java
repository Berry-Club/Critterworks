package dev.aaronhowser.mods.critterworks.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.aaronhowser.mods.critterworks.registry.ModDataComponents;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ItemUtils.class)
public abstract class ItemUtilsMixin {

	@WrapMethod(
		method = "createFilledResult(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/item/ItemStack;"
	)
	private static ItemStack critterworks$preserveTardigrade(
		ItemStack consumedStack,
		Player player,
		ItemStack resultStack,
		boolean preventDuplicates,
		Operation<ItemStack> original
	) {
		DataComponentType<Unit> tardigradeComponent = ModDataComponents.INSTANCE
			.getHAS_TARDIGRADE()
			.get();

		if (!resultStack.isEmpty() && consumedStack.has(tardigradeComponent)) {
			resultStack.set(tardigradeComponent, Unit.INSTANCE);
		}

		return original.call(consumedStack, player, resultStack, preventDuplicates);
	}
}