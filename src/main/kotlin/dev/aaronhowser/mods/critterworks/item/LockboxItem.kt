package dev.aaronhowser.mods.critterworks.item

import dev.aaronhowser.mods.aaron.misc.AaronExtensions.toGrayComponent
import dev.aaronhowser.mods.critterworks.client.item.lockbox.LockboxItemRenderer
import dev.aaronhowser.mods.critterworks.datagen.language.ModMenuLang
import net.minecraft.ChatFormatting
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.component.ItemContainerContents
import software.bernie.geckolib.animatable.GeoItem
import software.bernie.geckolib.animatable.client.GeoRenderProvider
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.animation.AnimatableManager
import software.bernie.geckolib.util.GeckoLibUtil
import java.util.function.Consumer

class LockboxItem(
	properties: Properties
) : Item(properties), GeoItem {

	private val animatableInstanceCache = GeckoLibUtil.createInstanceCache(this)

	override fun createGeoRenderer(consumer: Consumer<GeoRenderProvider>) {
		consumer.accept(object : GeoRenderProvider {
			private var renderer: LockboxItemRenderer? = null

			override fun getGeoItemRenderer(): BlockEntityWithoutLevelRenderer {
				var currentRenderer = renderer

				if (currentRenderer == null) {
					currentRenderer = LockboxItemRenderer()
					renderer = currentRenderer
				}

				return currentRenderer
			}
		})
	}

	override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar) = Unit

	override fun getAnimatableInstanceCache(): AnimatableInstanceCache = animatableInstanceCache

	override fun appendHoverText(
		stack: ItemStack,
		context: TooltipContext,
		tooltipComponents: MutableList<Component>,
		tooltipFlag: TooltipFlag
	) {
		val containerContents = stack.getOrDefault(
			DataComponents.CONTAINER,
			ItemContainerContents.EMPTY
		)

		val contentsText = when (
			val stackCount = containerContents.nonEmptyItemsCopy().count()
		) {
			0 -> ModMenuLang.CONTAINER_EMPTY.toGrayComponent()
			1 -> ModMenuLang.CONTAINER_STACK.toGrayComponent(stackCount)
			else -> ModMenuLang.CONTAINER_STACKS.toGrayComponent(stackCount)
		}

		tooltipComponents += contentsText.withStyle(ChatFormatting.GRAY)
	}

	companion object {
		val DEFAULT_PROPERTIES: () -> Properties = {
			Properties()
				.stacksTo(1)
				.component(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
		}
	}
}