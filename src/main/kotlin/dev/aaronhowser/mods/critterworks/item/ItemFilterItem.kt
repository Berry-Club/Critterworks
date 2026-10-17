package dev.aaronhowser.mods.critterworks.item

import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isNotEmpty
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isServerSide
import dev.aaronhowser.mods.critterworks.entity.filterfeeder.FilterfeederEntity
import dev.aaronhowser.mods.critterworks.item.component.ItemFilterComponent
import dev.aaronhowser.mods.critterworks.menu.item_filter.ItemFilterMenu
import dev.aaronhowser.mods.critterworks.registry.ModDataComponents
import net.minecraft.ChatFormatting
import net.minecraft.core.NonNullList
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
		for (flag in component.flags) {
			tooltipComponents.add(flag.getMessage(true).withStyle(ChatFormatting.BLUE))
		}

		val filterStacks = component.getItems()
		for (y in 0 until 4) {
			val stacks: MutableList<ItemStack> = mutableListOf()
			for (x in 0 until 4) {
				val ghostStack = filterStacks.getOrNull(y * 4 + x) ?: continue
				if (ghostStack.isNotEmpty()) stacks.add(ghostStack)
			}
			if (stacks.isEmpty()) continue

			val line = Component.empty()
			for (index in stacks.indices) {
				line.append(stacks[index].hoverName)
				if (index < stacks.lastIndex) line.append(Component.literal(", "))
			}
			tooltipComponents.add(line)
		}
	}

	companion object {
		fun getFilterComponent(filterStack: ItemStack): ItemFilterComponent {
			return filterStack.get(ModDataComponents.ITEM_FILTER) ?: ItemFilterComponent()
		}

		fun setFlags(filterStack: ItemStack, flags: List<ItemFilterComponent.Flag>) {
			filterStack.set(ModDataComponents.ITEM_FILTER, getFilterComponent(filterStack).withFlags(flags))
		}

		fun setStack(filterStack: ItemStack, slot: Int, stackToPlace: ItemStack): Boolean {
			if (slot !in 0 until ItemFilterComponent.CONTAINER_SIZE) return false
			filterStack.set(
				ModDataComponents.ITEM_FILTER,
				getFilterComponent(filterStack).withSetItem(slot, stackToPlace)
			)
			return true
		}

		fun getFilterItems(filterStack: ItemStack): NonNullList<ItemStack> {
			return getFilterComponent(filterStack).getItems()
		}

		fun passesFilter(filterStack: ItemStack, checkedStack: ItemStack): Boolean {
			return getFilterComponent(filterStack).passesFilter(checkedStack)
		}
	}
}