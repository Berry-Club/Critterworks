package dev.aaronhowser.mods.critterworks.client.entity.punchprawn

import dev.aaronhowser.mods.critterworks.entity.punchprawn.PunchprawnEntity
import net.minecraft.client.renderer.entity.EntityRendererProvider
import software.bernie.geckolib.renderer.GeoEntityRenderer

class PunchprawnRenderer(
	context: EntityRendererProvider.Context
) : GeoEntityRenderer<PunchprawnEntity>(context, PunchprawnModel())