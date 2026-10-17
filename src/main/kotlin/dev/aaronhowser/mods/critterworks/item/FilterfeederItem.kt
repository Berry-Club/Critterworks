package dev.aaronhowser.mods.critterworks.item

import dev.aaronhowser.mods.critterworks.client.render.item.FilterfeederItemRenderer
import dev.aaronhowser.mods.critterworks.entity.filterfeeder.FilterfeederEntity
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer
import net.minecraft.world.item.Item
import software.bernie.geckolib.animatable.GeoItem
import software.bernie.geckolib.animatable.client.GeoRenderProvider
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.animation.AnimatableManager
import software.bernie.geckolib.util.GeckoLibUtil
import java.util.function.Consumer

abstract class FilterfeederItem(
	properties: Properties,
	val filterType: FilterfeederEntity.FilterType
) : Item(properties), GeoItem {

	private val animatableInstanceCache = GeckoLibUtil.createInstanceCache(this)

	override fun createGeoRenderer(consumer: Consumer<GeoRenderProvider>) {
		consumer.accept(object : GeoRenderProvider {
			private var renderer: FilterfeederItemRenderer? = null

			override fun getGeoItemRenderer(): BlockEntityWithoutLevelRenderer {
				var currentRenderer = renderer

				if (currentRenderer == null) {
					currentRenderer = FilterfeederItemRenderer()
					renderer = currentRenderer
				}

				return currentRenderer
			}
		})
	}

	override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar) = Unit

	override fun getAnimatableInstanceCache(): AnimatableInstanceCache = animatableInstanceCache

}