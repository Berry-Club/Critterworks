package dev.aaronhowser.mods.critterworks.client.entity.punchprawn

import dev.aaronhowser.mods.critterworks.Critterworks
import dev.aaronhowser.mods.critterworks.entity.punchprawn.PunchprawnEntity
import software.bernie.geckolib.model.DefaultedEntityGeoModel

class PunchprawnModel : DefaultedEntityGeoModel<PunchprawnEntity>(
	Critterworks.modResource("punchprawn")
)