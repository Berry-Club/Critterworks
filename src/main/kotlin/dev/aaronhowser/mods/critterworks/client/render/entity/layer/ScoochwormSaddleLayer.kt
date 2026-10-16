package dev.aaronhowser.mods.critterworks.client.render.entity.layer

import dev.aaronhowser.mods.critterworks.client.model.entity.ScoochwormSaddleModel
import dev.aaronhowser.mods.critterworks.entity.scoochworm.ScoochwormPartEntity
import dev.aaronhowser.mods.critterworks.registry.ModScoochwormAttachmentTypes
import software.bernie.geckolib.renderer.GeoRenderer

class ScoochwormSaddleLayer(
	renderer: GeoRenderer<ScoochwormPartEntity>
) : ScoochwormPartAttachmentLayer(
	renderer,
	ModScoochwormAttachmentTypes.SADDLE.get(),
	ScoochwormSaddleModel()
)