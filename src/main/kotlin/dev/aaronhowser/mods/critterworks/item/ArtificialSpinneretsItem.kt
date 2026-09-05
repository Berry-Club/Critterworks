package dev.aaronhowser.mods.critterworks.item

import dev.aaronhowser.mods.aaron.misc.AaronExtensions.toComponent
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.toGrayComponent
import dev.aaronhowser.mods.critterworks.datagen.language.ModItemLang
import dev.aaronhowser.mods.critterworks.datagen.language.ModMenuLang
import dev.aaronhowser.mods.critterworks.handler.web.WebLineInteractionHandler
import dev.aaronhowser.mods.critterworks.registry.ModDataComponents
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.SlotAccess
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.ClickAction
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.Level
import java.util.*
import java.util.function.Supplier
import kotlin.math.ceil

class ArtificialSpinneretsItem(properties: Properties) : Item(properties) {

	override fun useOn(context: UseOnContext): InteractionResult {
		val level = context.level
		if (level !is ServerLevel) return InteractionResult.SUCCESS

		val player = context.player ?: return InteractionResult.PASS
		val blockAnchor = WebLineInteractionHandler.createBlockAnchor(
			context.clickedPos,
			context.clickedFace,
			context.clickLocation
		)

		WebLineInteractionHandler.handleNodeSelection(level, player, context.itemInHand, blockAnchor)
		return InteractionResult.CONSUME
	}

	override fun use(
		level: Level,
		player: Player,
		usedHand: InteractionHand
	): InteractionResultHolder<ItemStack> {
		if (player.isSecondaryUseActive) {
			val usedStack = player.getItemInHand(usedHand)
			if (usedStack.has(ModDataComponents.WEB_NODE)) {
				usedStack.remove(ModDataComponents.WEB_NODE)
				return InteractionResultHolder.sidedSuccess(usedStack, level.isClientSide)
			}
		}

		return super.use(level, player, usedHand)
	}

	override fun isFoil(stack: ItemStack): Boolean {
		return stack.has(ModDataComponents.WEB_NODE)
	}

	override fun overrideOtherStackedOnMe(
		thisStack: ItemStack,
		otherStack: ItemStack,
		slot: Slot,
		action: ClickAction,
		player: Player,
		access: SlotAccess
	): Boolean {
		if (action != ClickAction.PRIMARY || !slot.allowModification(player)) return false
		return insertString(thisStack, otherStack)
	}

	override fun overrideStackedOnOther(
		thisStack: ItemStack,
		slot: Slot,
		action: ClickAction,
		player: Player
	): Boolean {
		if (action != ClickAction.PRIMARY || !slot.allowModification(player)) return false
		return insertString(thisStack, slot.item)
	}

	private fun insertString(spinnerets: ItemStack, stringStack: ItemStack): Boolean {
		if (stringStack.item !== Items.STRING) return false

		val webFluid = spinnerets.getOrDefault(ModDataComponents.WEB_FLUID, 0.0)
		val availableCapacity = MAX_WEB_FLUID - webFluid
		if (availableCapacity <= 0.0) return false

		val stringsToInsert = minOf(stringStack.count, ceil(availableCapacity).toInt())
		val insertedWebFluid = minOf(stringsToInsert.toDouble(), availableCapacity)

		spinnerets.set(ModDataComponents.WEB_FLUID, webFluid + insertedWebFluid)
		stringStack.shrink(stringsToInsert)
		return true
	}

	override fun appendHoverText(
		stack: ItemStack,
		context: TooltipContext,
		tooltipComponents: MutableList<Component>,
		tooltipFlag: TooltipFlag
	) {
		val webFluid = stack.getOrDefault(ModDataComponents.WEB_FLUID, 0.0)
		val formattedWebFluid = String.format(Locale.ROOT, "%.2f", webFluid)

		tooltipComponents += ModMenuLang.WEB_FLUID.toGrayComponent(
			formattedWebFluid,
			MAX_WEB_FLUID.toInt()
		)

		if (webFluid >= MAX_WEB_FLUID) return

		tooltipComponents += if (tooltipFlag.hasShiftDown()) {
			ModMenuLang.WEB_FLUID_INSTRUCTIONS
				.toGrayComponent()
		} else {
			ModMenuLang.SHIFT_FOR_INFO
				.toComponent()
				.withStyle(ChatFormatting.DARK_GRAY)
		}
	}

	companion object {
		const val MAX_WEB_FLUID = 128.0

		val DEFAULT_PROPERTIES: Supplier<Properties> =
			Supplier {
				Properties()
					.stacksTo(1)
					.component(ModDataComponents.WEB_FLUID.get(), 0.0)
			}
	}

}