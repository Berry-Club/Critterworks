package dev.aaronhowser.mods.critterworks.entity.filterfeeder.goal

import dev.aaronhowser.mods.critterworks.entity.filterfeeder.FilterfeederEntity
import net.minecraft.tags.ItemTags
import net.minecraft.world.entity.ai.goal.TemptGoal
import net.minecraft.world.item.crafting.Ingredient

class FilterfeederWaitForSandGoal(
	private val filterfeeder: FilterfeederEntity
) : TemptGoal(filterfeeder, 1.0, Ingredient.of(ItemTags.SAND), false) {

	override fun tick() {
		super.tick()
		filterfeeder.navigation.stop()
	}

}