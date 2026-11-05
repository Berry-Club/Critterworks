package dev.aaronhowser.mods.critterworks.item

import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isServerSide
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.toGrayComponent
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.toComponent
import dev.aaronhowser.mods.critterworks.datagen.language.ModMenuLang
import dev.aaronhowser.mods.critterworks.entity.filterfeeder.FilterfeederEntity
import dev.aaronhowser.mods.critterworks.item.component.ItemFilterComponent
import dev.aaronhowser.mods.critterworks.item.component.ItemFilterEntry
import dev.aaronhowser.mods.critterworks.menu.item_filter.ItemFilterMenu
import dev.aaronhowser.mods.critterworks.registry.ModDataComponents
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.SimpleMenuProvider
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.MenuConstructor
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.Level

class ItemFilterItem(properties: Properties) : FilterfeederItem(
	properties,
	FilterfeederEntity.FilterType.ITEM
) {

	override fun use(
		level: Level,
		player: Player,
		usedHand: InteractionHand
	): InteractionResultHolder<ItemStack?> {
		val usedStack = player.getItemInHand(usedHand)

		if (level.isServerSide) {
			val constructor = MenuConstructor { containerId, inventory, _ ->
				ItemFilterMenu(containerId, inventory, usedHand)
			}

			player.openMenu(SimpleMenuProvider(constructor, usedStack.hoverName)) { it.writeEnum(usedHand) }
		}

		return InteractionResultHolder.sidedSuccess(usedStack, level.isClientSide)
	}

	override fun appendHoverText(
		stack: ItemStack,
		context: TooltipContext,
		tooltipComponents: MutableList<Component>,
		tooltipFlag: TooltipFlag
	) {
		val component = getFilterComponent(stack)

		if (component.isBlacklist) {
			tooltipComponents.add(
				ModMenuLang.BLACKLIST
					.toComponent()
					.withStyle(ChatFormatting.RED, ChatFormatting.UNDERLINE)
			)
		}

		for (entry in component.entries) {
			if (entry is ItemFilterEntry.Empty) continue

			val displayStack = entry.getDisplayStack(context.registries() ?: continue)
			tooltipComponents.add(ModMenuLang.FILTER_LIST_POINT.toGrayComponent(displayStack.hoverName))
		}
	}

	companion object {
		fun getFilterComponent(filterStack: ItemStack): ItemFilterComponent {
			return filterStack.get(ModDataComponents.ITEM_FILTER) ?: ItemFilterComponent()
		}

		fun passesFilter(filterStack: ItemStack, checkedStack: ItemStack): Boolean {
			return getFilterComponent(filterStack).test(checkedStack)
		}
	}
}