package dev.aaronhowser.mods.critterworks.client.entity.parry_platypus

import dev.aaronhowser.mods.critterworks.entity.parry_platypus.ParryPlatypusEntity
import net.minecraft.client.renderer.entity.EntityRendererProvider
import software.bernie.geckolib.renderer.GeoEntityRenderer

class ParryPlatypusRenderer(
	context: EntityRendererProvider.Context
) : GeoEntityRenderer<ParryPlatypusEntity>(context, ParryPlatypusModel())