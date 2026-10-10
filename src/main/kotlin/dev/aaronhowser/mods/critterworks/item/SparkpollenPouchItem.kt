package dev.aaronhowser.mods.critterworks.item

import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isBlock
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isServerSide
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.tell
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.toBlockPos
import dev.aaronhowser.mods.critterworks.registry.ModBlockEntityTypes
import dev.aaronhowser.mods.critterworks.registry.ModBlocks
import dev.aaronhowser.mods.critterworks.registry.ModDataComponents
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.Level
import kotlin.jvm.optionals.getOrNull

class SparkpollenPouchItem(properties: Properties) : Item(properties) {

	override fun use(
		level: Level,
		player: Player,
		usedHand: InteractionHand
	): InteractionResultHolder<ItemStack> {
		if (!player.isSecondaryUseActive) return super.use(level, player, usedHand)

		val usedStack = player.getItemInHand(usedHand)
		val isInput = usedStack.getOrDefault(ModDataComponents.IS_INPUT, true)
		usedStack.set(ModDataComponents.IS_INPUT, !isInput)

		return InteractionResultHolder.sidedSuccess(usedStack, level.isClientSide)
	}

	override fun useOn(context: UseOnContext): InteractionResult {
		val level = context.level
		val pos = context.clickedPos

		val stack = context.itemInHand

		val clickedState = level.getBlockState(pos)
		if (clickedState.isBlock(ModBlocks.SPARKBUG_BUSH)) {
			stack.set(ModDataComponents.SPARKBUG_BUSH, pos.asLong())
			return InteractionResult.SUCCESS
		}

		val bushPos = stack.get(ModDataComponents.SPARKBUG_BUSH)
			?.toBlockPos()
			?: return InteractionResult.PASS
		val bushBe = level.getBlockEntity(bushPos, ModBlockEntityTypes.SPARKBUG_BUSH.get())
			.getOrNull()
			?: return InteractionResult.PASS

		if (level.isServerSide) {
			val clickedDirection = context.clickedFace
			val isInput = stack.getOrDefault(ModDataComponents.IS_INPUT, true)

			val player = context.player

			val added = bushBe.addPollenSpot(pos, clickedDirection, isInput)
			if (added) {
				if (isInput) {
					player?.tell("Added a Charging Sparkpollen Pinch")
				} else {
					player?.tell("Added a Grounding Sparkpollen Pinch")
				}
			} else {
				val removed = bushBe.removePollenSpot(pos, clickedDirection)
				if (removed) {
					if (isInput) {
						player?.tell("Removed a Charging Sparkpollen Pinch")
					} else {
						player?.tell("Removed a Grounding Sparkpollen Pinch")
					}
				}
			}
		}

		return InteractionResult.SUCCESS
	}

	companion object {
		val DEFAULT_PROPERTIES = {
			Properties()
				.stacksTo(1)
				.component(ModDataComponents.SPARKBUG_BUSH, 0)
				.component(ModDataComponents.IS_INPUT, true)
		}
	}

}