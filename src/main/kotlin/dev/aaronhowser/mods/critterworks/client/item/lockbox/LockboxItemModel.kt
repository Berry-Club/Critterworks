package dev.aaronhowser.mods.critterworks.client.item.lockbox

import dev.aaronhowser.mods.critterworks.Critterworks
import dev.aaronhowser.mods.critterworks.item.LockboxItem
import net.minecraft.resources.ResourceLocation
import software.bernie.geckolib.model.DefaultedItemGeoModel

class LockboxItemModel : DefaultedItemGeoModel<LockboxItem>(
	Critterworks.modResource("lockbox")
) {

	override fun getModelResource(animatable: LockboxItem): ResourceLocation =
		Critterworks.modResource("geo/entity/scoochworm/lockbox.geo.json")

	override fun getTextureResource(animatable: LockboxItem): ResourceLocation =
		Critterworks.modResource("textures/entity/scoochworm/lockbox.png")

}