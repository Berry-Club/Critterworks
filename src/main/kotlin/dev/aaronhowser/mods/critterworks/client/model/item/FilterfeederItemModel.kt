package dev.aaronhowser.mods.critterworks.client.model.item

import dev.aaronhowser.mods.critterworks.Critterworks
import dev.aaronhowser.mods.critterworks.entity.filterfeeder.FilterfeederEntity
import dev.aaronhowser.mods.critterworks.item.FilterfeederItem
import net.minecraft.resources.ResourceLocation
import software.bernie.geckolib.model.DefaultedItemGeoModel

class FilterfeederItemModel : DefaultedItemGeoModel<FilterfeederItem>(
	Critterworks.modResource("filterfeeder")
) {

	override fun getModelResource(animatable: FilterfeederItem): ResourceLocation =
		Critterworks.modResource("geo/entity/filterfeeder.geo.json")

	override fun getTextureResource(animatable: FilterfeederItem): ResourceLocation {
		return when (animatable.filterType) {
			FilterfeederEntity.FilterType.ITEM -> ITEM_TEXTURE
			FilterfeederEntity.FilterType.FLUID -> FLUID_TEXTURE
		}
	}

	companion object {
		private val ITEM_TEXTURE = Critterworks.modResource("textures/entity/item_filterfeeder.png")
		private val FLUID_TEXTURE = Critterworks.modResource("textures/entity/fluid_filterfeeder.png")
	}

}