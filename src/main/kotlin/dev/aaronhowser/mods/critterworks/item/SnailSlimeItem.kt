package dev.aaronhowser.mods.critterworks.item

import net.minecraft.world.InteractionResult
import net.minecraft.world.item.BoneMealItem
import net.minecraft.world.item.context.UseOnContext

//TODO: When sneaking, instead place a block of Snail Slime that acts like Honey Blocks, slowing you down
class SnailSlimeItem(properties: Properties) : BoneMealItem(properties) {

	override fun useOn(context: UseOnContext): InteractionResult {
		val player = context.player
		if (player != null && player.isSecondaryUseActive) return InteractionResult.PASS

		return super.useOn(context)
	}

}