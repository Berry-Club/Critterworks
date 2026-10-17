package dev.aaronhowser.mods.critterworks.client.model.entity

import dev.aaronhowser.mods.critterworks.Critterworks
import dev.aaronhowser.mods.critterworks.entity.filterfeeder.FilterfeederEntity
import net.minecraft.resources.ResourceLocation
import software.bernie.geckolib.model.DefaultedEntityGeoModel

class FilterfeederModel : DefaultedEntityGeoModel<FilterfeederEntity>(
	Critterworks.modResource("filterfeeder")
) {

	override fun getTextureResource(animatable: FilterfeederEntity): ResourceLocation {
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