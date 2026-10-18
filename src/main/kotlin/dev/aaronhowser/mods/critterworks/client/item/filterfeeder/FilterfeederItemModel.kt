package dev.aaronhowser.mods.critterworks.client.item.filterfeeder

import dev.aaronhowser.mods.critterworks.Critterworks
import dev.aaronhowser.mods.critterworks.entity.filterfeeder.FilterfeederEntity
import dev.aaronhowser.mods.critterworks.item.FilterfeederItem
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.Mth
import software.bernie.geckolib.animation.AnimationState
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

	override fun setCustomAnimations(
		animatable: FilterfeederItem,
		instanceId: Long,
		animationState: AnimationState<FilterfeederItem>
	) {
		val top = animationProcessor.getBone("top")
		top.rotX = Mth.HALF_PI / 2f
	}

	companion object {
		private val ITEM_TEXTURE = Critterworks.modResource("textures/entity/item_filterfeeder.png")
		private val FLUID_TEXTURE = Critterworks.modResource("textures/entity/fluid_filterfeeder.png")
	}

}