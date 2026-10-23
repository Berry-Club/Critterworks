package dev.aaronhowser.mods.critterworks.client.entity.sproutsnail

import dev.aaronhowser.mods.critterworks.entity.sproutsnail.SproutsnailEntity
import net.minecraft.client.renderer.entity.EntityRendererProvider
import software.bernie.geckolib.renderer.GeoEntityRenderer

class SproutsnailRenderer(
	context: EntityRendererProvider.Context
) : GeoEntityRenderer<SproutsnailEntity>(context, SproutsnailModel())