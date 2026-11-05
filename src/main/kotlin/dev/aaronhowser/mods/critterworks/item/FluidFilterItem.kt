package dev.aaronhowser.mods.critterworks.item

import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isServerSide
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.toComponent
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.toGrayComponent
import dev.aaronhowser.mods.critterworks.datagen.language.ModMenuLang
import dev.aaronhowser.mods.critterworks.entity.filterfeeder.FilterfeederEntity
import dev.aaronhowser.mods.critterworks.item.component.FluidFilterComponent
import dev.aaronhowser.mods.critterworks.item.component.FluidFilterEntry
import dev.aaronhowser.mods.critterworks.menu.fluid_filter.FluidFilterMenu
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
import net.neoforged.neoforge.fluids.FluidStack
import net.neoforged.neoforge.fluids.FluidUtil

class FluidFilterItem(properties: Properties) : FilterfeederItem(
	properties,
	FilterfeederEntity.FilterType.FLUID
) {

	override fun use(
		level: Level,
		player: Player,
		usedHand: InteractionHand
	): InteractionResultHolder<ItemStack?> {
		val usedStack = player.getItemInHand(usedHand)

		if (level.isServerSide) {
			val menuConstructor = MenuConstructor { containerId, playerInventory, _ ->
				FluidFilterMenu(containerId, playerInventory, usedHand)
			}

			val provider = SimpleMenuProvider(menuConstructor, usedStack.hoverName)
			player.openMenu(provider) { data -> data.writeEnum(usedHand) }
		}

		return InteractionResultHolder.sidedSuccess(usedStack, level.isClientSide)
	}

	override fun appendHoverText(
		stack: ItemStack,
		context: TooltipContext,
		tooltipComponents: MutableList<Component>,
		tooltipFlag: TooltipFlag
	) {
		val filterComponent = getFilterComponent(stack)

		if (filterComponent.isBlacklist) {
			tooltipComponents.add(
				ModMenuLang.BLACKLIST
					.toComponent()
					.withStyle(ChatFormatting.RED, ChatFormatting.UNDERLINE)
			)
		}

		for (entry in filterComponent.entries) {
			if (entry is FluidFilterEntry.Empty) continue

			val name = when (entry) {
				is FluidFilterEntry.SpecificFluid -> {
					FluidUtil.getFluidContained(entry.containerStack)
						.orElse(FluidStack.EMPTY)
						.hoverName
				}
				is FluidFilterEntry.Tag -> Component.literal("#${entry.tagKey.location()}")
				FluidFilterEntry.Empty -> continue
			}

			tooltipComponents.add(ModMenuLang.FILTER_LIST_POINT.toGrayComponent(name))
		}
	}

	companion object {
		fun getFilterComponent(filterStack: ItemStack): FluidFilterComponent {
			return filterStack.get(ModDataComponents.FLUID_FILTER) ?: FluidFilterComponent()
		}

		fun passesFilter(filterStack: ItemStack, checkedStack: FluidStack): Boolean {
			val component = getFilterComponent(filterStack)
			return component.test(checkedStack)
		}
	}
}