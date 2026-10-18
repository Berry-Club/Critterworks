package dev.aaronhowser.mods.critterworks.client.model.entity

import dev.aaronhowser.mods.critterworks.Critterworks
import dev.aaronhowser.mods.critterworks.client.render.entity.SnailShellRenderer
import software.bernie.geckolib.model.DefaultedGeoModel

class SnailShellModel : DefaultedGeoModel<SnailShellRenderer.ShellAnimatable>(
	Critterworks.modResource("snail_shell")
) {
	override fun subtype(): String = "entity"
}