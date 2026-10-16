package dev.aaronhowser.mods.critterworks.item

import dev.aaronhowser.mods.critterworks.block.DyeberryVinesBlock
import dev.aaronhowser.mods.critterworks.entity.scoochworm.data.WormColor
import dev.aaronhowser.mods.critterworks.registry.ModDataComponents
import dev.aaronhowser.mods.critterworks.registry.ModMobEffects
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.food.FoodProperties
import net.minecraft.world.item.ItemNameBlockItem
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState

class DyeberryItem(
	block: Block,
	private val wormColor: WormColor,
	properties: Properties
) : ItemNameBlockItem(block, properties) {

	override fun getPlacementState(context: BlockPlaceContext): BlockState? {
		val placementState = super.getPlacementState(context) ?: return null

		return placementState.setValue(DyeberryVinesBlock.COLOR, wormColor)
	}

	companion object {
		const val EAT_DURATION = 20 * 60
		const val POTION_DURATION = 20 * 60 * 5

		fun getProperties(wormColor: WormColor): Properties {
			val foodProperties = FoodProperties.Builder()
				.nutrition(2)
				.saturationModifier(0.1f)
				.alwaysEdible()
				.effect(
					{
						MobEffectInstance(
							ModMobEffects.getDyedEffect(wormColor),
							EAT_DURATION,
							0,
							false,
							false,
							true
						)
					},
					1f
				)
				.build()

			return Properties()
				.food(foodProperties)
				.component(ModDataComponents.WORM_COLOR, wormColor)
		}
	}

}