package dev.aaronhowser.mods.critterworks.client.model.entity

import dev.aaronhowser.mods.critterworks.Critterworks
import dev.aaronhowser.mods.critterworks.entity.scoochworm.ScoochwormPartEntity
import software.bernie.geckolib.model.DefaultedEntityGeoModel

class ScoochwormSaddleModel : DefaultedEntityGeoModel<ScoochwormPartEntity>(
	Critterworks.modResource("scoochworm/saddle")
)