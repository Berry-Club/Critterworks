package dev.aaronhowser.mods.critterworks.client.entity.snail_shell

import dev.aaronhowser.mods.critterworks.Critterworks
import software.bernie.geckolib.model.DefaultedGeoModel

class SnailShellModel : DefaultedGeoModel<SnailShellRenderer.ShellAnimatable>(
	Critterworks.modResource("snail_shell")
) {
	override fun subtype(): String = "entity"
}