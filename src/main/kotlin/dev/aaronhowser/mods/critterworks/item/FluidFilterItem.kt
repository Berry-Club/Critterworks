package dev.aaronhowser.mods.critterworks.item

import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isServerSide
import dev.aaronhowser.mods.critterworks.entity.filterfeeder.FilterfeederEntity
import dev.aaronhowser.mods.critterworks.item.component.FluidFilterComponent
import dev.aaronhowser.mods.critterworks.menu.fluid_filter.FluidFilterMenu
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

		for (flag in filterComponent.flags) {
			val component = flag.getMessage(true).withStyle(ChatFormatting.BLUE)
			tooltipComponents.add(component)
		}

		val filterStacks = filterComponent.getItems()

		for (y in 0 until 4) {
			val stacks = mutableListOf<FluidStack>()

			for (x in 0 until 4) {
				val slot = y * 4 + x
				val ghostStack = filterStacks.getOrNull(slot) ?: continue
				val fluidStack = FluidUtil.getFluidContained(ghostStack).orElse(FluidStack.EMPTY)

				if (!fluidStack.isEmpty) {
					stacks.add(fluidStack)
				}
			}

			if (stacks.isEmpty()) continue

			val component = Component.empty()
			for ((index, fluidStack) in stacks.withIndex()) {
				component.append(fluidStack.hoverName)

				if (index < stacks.size - 1) {
					component.append(Component.literal(", "))
				}
			}

			tooltipComponents.add(component)
		}
	}

	companion object {
		fun getFilterComponent(filterStack: ItemStack): FluidFilterComponent {
			return filterStack.get(ModDataComponents.FLUID_FILTER) ?: FluidFilterComponent()
		}

		fun setFlags(filterStack: ItemStack, flags: List<FluidFilterComponent.Flag>) {
			val currentComponent = getFilterComponent(filterStack)
			val newComponent = currentComponent.withFlags(flags)
			filterStack.set(ModDataComponents.FLUID_FILTER, newComponent)
		}

		fun setStack(filterStack: ItemStack, slot: Int, stackToPlace: ItemStack): Boolean {
			if (slot !in 0 until FluidFilterComponent.CONTAINER_SIZE) return false

			val currentComponent = getFilterComponent(filterStack)
			val newComponent = currentComponent.withSetItem(slot, stackToPlace)

			filterStack.set(ModDataComponents.FLUID_FILTER, newComponent)
			return true
		}

		fun getFilterItems(filterStack: ItemStack): NonNullList<ItemStack> {
			val component = getFilterComponent(filterStack)

			return component.getItems()
		}

		fun passesFilter(filterStack: ItemStack, checkedStack: FluidStack): Boolean {
			val component = getFilterComponent(filterStack)
			return component.passesFilter(checkedStack)
		}
	}
}