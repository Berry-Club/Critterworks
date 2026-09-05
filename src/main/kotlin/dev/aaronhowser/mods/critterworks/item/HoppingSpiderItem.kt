package dev.aaronhowser.mods.critterworks.item

import dev.aaronhowser.mods.aaron.misc.AaronExtensions.toComponent
import dev.aaronhowser.mods.critterworks.client.render.item.HoppingSpiderItemRenderer
import dev.aaronhowser.mods.critterworks.handler.spider.HoppingSpider
import dev.aaronhowser.mods.critterworks.registry.ModItems
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import software.bernie.geckolib.animatable.GeoItem
import software.bernie.geckolib.animatable.SingletonGeoAnimatable
import software.bernie.geckolib.animatable.client.GeoRenderProvider
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.animation.AnimatableManager
import software.bernie.geckolib.util.GeckoLibUtil
import java.util.function.Consumer

class HoppingSpiderItem(properties: Properties) : Item(properties), GeoItem {

	private val animatableInstanceCache = GeckoLibUtil.createInstanceCache(this)

	init {
		SingletonGeoAnimatable.registerSyncedAnimatable(this)
	}

	override fun createGeoRenderer(consumer: Consumer<GeoRenderProvider>) {
		consumer.accept(object : GeoRenderProvider {
			private var renderer: HoppingSpiderItemRenderer? = null

			override fun getGeoItemRenderer(): BlockEntityWithoutLevelRenderer {
				var currentRenderer = renderer

				if (currentRenderer == null) {
					currentRenderer = HoppingSpiderItemRenderer()
					renderer = currentRenderer
				}

				return currentRenderer
			}
		})
	}

	override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar) {}

	override fun getAnimatableInstanceCache(): AnimatableInstanceCache = animatableInstanceCache

	companion object {
		fun createSpider(stack: ItemStack): HoppingSpider {
			val customName = stack.get(DataComponents.CUSTOM_NAME)?.string
			return HoppingSpider(customName = customName)
		}

		fun createStack(spider: HoppingSpider): ItemStack {
			val stack = ModItems.HOPPING_SPIDER.toStack()
			val customName = spider.customName

			if (customName != null) {
				stack.set(DataComponents.CUSTOM_NAME, customName.toComponent())
			}

			return stack
		}
	}
}