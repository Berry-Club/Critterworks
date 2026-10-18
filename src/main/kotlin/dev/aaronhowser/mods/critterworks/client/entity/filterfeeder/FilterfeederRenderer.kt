package dev.aaronhowser.mods.critterworks.client.entity.filterfeeder

import dev.aaronhowser.mods.critterworks.entity.filterfeeder.FilterfeederEntity
import net.minecraft.client.renderer.entity.EntityRendererProvider
import software.bernie.geckolib.renderer.GeoEntityRenderer

class FilterfeederRenderer(
	context: EntityRendererProvider.Context
) : GeoEntityRenderer<FilterfeederEntity>(context, FilterfeederModel())