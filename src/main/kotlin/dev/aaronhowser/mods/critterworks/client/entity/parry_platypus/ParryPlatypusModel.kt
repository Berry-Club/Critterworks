package dev.aaronhowser.mods.critterworks.client.entity.parry_platypus

import dev.aaronhowser.mods.critterworks.Critterworks
import dev.aaronhowser.mods.critterworks.entity.parry_platypus.ParryPlatypusEntity
import software.bernie.geckolib.model.DefaultedEntityGeoModel

class ParryPlatypusModel : DefaultedEntityGeoModel<ParryPlatypusEntity>(
	Critterworks.modResource("parry_platypus"),
	"head"
)