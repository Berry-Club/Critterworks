package dev.aaronhowser.mods.critterworks.client.entity.sproutsnail

import dev.aaronhowser.mods.critterworks.Critterworks
import dev.aaronhowser.mods.critterworks.entity.sproutsnail.SproutsnailEntity
import software.bernie.geckolib.model.DefaultedEntityGeoModel

class SproutsnailModel : DefaultedEntityGeoModel<SproutsnailEntity>(
	Critterworks.modResource("sproutsnail")
)