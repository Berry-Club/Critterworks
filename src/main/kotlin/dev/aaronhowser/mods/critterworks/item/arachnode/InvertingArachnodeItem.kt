package dev.aaronhowser.mods.critterworks.item.arachnode

import dev.aaronhowser.mods.critterworks.client.item.inverting_arachnode.InvertingArachnodeItemRenderer
import dev.aaronhowser.mods.critterworks.handler.web.redstone.node.Arachnode
import dev.aaronhowser.mods.critterworks.handler.web.redstone.node.ArachnodeMount
import dev.aaronhowser.mods.critterworks.handler.web.redstone.node.InvertingArachnode
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer
import software.bernie.geckolib.animatable.GeoItem
import software.bernie.geckolib.animatable.client.GeoRenderProvider
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.animation.AnimatableManager
import software.bernie.geckolib.util.GeckoLibUtil
import java.util.*
import java.util.function.Consumer

class InvertingArachnodeItem(properties: Properties) : ArachnodeItem(properties), GeoItem {

	private val animatableInstanceCache = GeckoLibUtil.createInstanceCache(this)

	override fun createGeoRenderer(consumer: Consumer<GeoRenderProvider>) {
		consumer.accept(object : GeoRenderProvider {
			private val renderer by lazy { InvertingArachnodeItemRenderer() }

			override fun getGeoItemRenderer(): BlockEntityWithoutLevelRenderer {
				return renderer
			}
		})
	}

	override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar) = Unit

	override fun getAnimatableInstanceCache(): AnimatableInstanceCache = animatableInstanceCache

	override fun createArachnode(context: ArachnodePlacementContext): Arachnode? {
		val arachnodeUuid = UUID.randomUUID()
		val ports = createDefaultPorts(context, arachnodeUuid) ?: return null

		val mount = when (context) {
			is ArachnodePlacementContext.OnLine -> ArachnodeMount.Line
			is ArachnodePlacementContext.OnSurface -> context.mount
		}

		return InvertingArachnode(
			arachnodeUuid,
			context.position,
			context.itemStack,
			mount,
			listOf(ports.inputPort),
			listOf(ports.outputPort)
		)
	}
}