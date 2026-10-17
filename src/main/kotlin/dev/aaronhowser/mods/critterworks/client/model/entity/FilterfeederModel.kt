package dev.aaronhowser.mods.critterworks.client.model.entity

import dev.aaronhowser.mods.critterworks.Critterworks
import dev.aaronhowser.mods.critterworks.entity.filterfeeder.FilterfeederEntity
import software.bernie.geckolib.model.DefaultedEntityGeoModel

class FilterfeederModel : DefaultedEntityGeoModel<FilterfeederEntity>(
	Critterworks.modResource("filterfeeder")
)