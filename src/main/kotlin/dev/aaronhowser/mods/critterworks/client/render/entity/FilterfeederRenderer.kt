package dev.aaronhowser.mods.critterworks.client.render.entity

import dev.aaronhowser.mods.critterworks.client.model.entity.FilterfeederModel
import dev.aaronhowser.mods.critterworks.entity.filterfeeder.FilterfeederEntity
import net.minecraft.client.renderer.entity.EntityRendererProvider
import software.bernie.geckolib.renderer.GeoEntityRenderer

class FilterfeederRenderer(
	context: EntityRendererProvider.Context
) : GeoEntityRenderer<FilterfeederEntity>(context, FilterfeederModel())