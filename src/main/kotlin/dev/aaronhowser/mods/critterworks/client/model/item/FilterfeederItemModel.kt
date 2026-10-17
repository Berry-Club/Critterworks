package dev.aaronhowser.mods.critterworks.client.model.item

import dev.aaronhowser.mods.critterworks.Critterworks
import dev.aaronhowser.mods.critterworks.item.FilterfeederItem
import net.minecraft.resources.ResourceLocation
import software.bernie.geckolib.model.DefaultedItemGeoModel

class FilterfeederItemModel : DefaultedItemGeoModel<FilterfeederItem>(
	Critterworks.modResource("filterfeeder")
) {

	override fun getModelResource(animatable: FilterfeederItem): ResourceLocation =
		Critterworks.modResource("geo/entity/filterfeeder.geo.json")

	override fun getTextureResource(animatable: FilterfeederItem): ResourceLocation =
		Critterworks.modResource("textures/entity/filterfeeder.png")

}