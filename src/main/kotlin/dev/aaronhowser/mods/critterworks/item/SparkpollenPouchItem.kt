package dev.aaronhowser.mods.critterworks.item

import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isBlock
import dev.aaronhowser.mods.critterworks.registry.ModBlocks
import dev.aaronhowser.mods.critterworks.registry.ModDataComponents
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.Item
import net.minecraft.world.item.context.UseOnContext

class SparkpollenPouchItem(properties: Properties) : Item(properties) {

	override fun useOn(context: UseOnContext): InteractionResult {
		val level = context.level
		val pos = context.clickedPos

		val clickedState = level.getBlockState(pos)
		if (clickedState.isBlock(ModBlocks.SPARKBUG_BUSH)) {
			context.itemInHand.set(ModDataComponents.SPARKBUG_BUSH, pos.asLong())
			return InteractionResult.SUCCESS
		}

		return InteractionResult.PASS
	}

	companion object {
		val DEFAULT_PROPERTIES = {
			Properties()
				.stacksTo(1)
				.component(ModDataComponents.SPARKBUG_BUSH, 0)
		}
	}

}