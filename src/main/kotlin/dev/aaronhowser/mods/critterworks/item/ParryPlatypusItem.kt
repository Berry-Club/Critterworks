package dev.aaronhowser.mods.critterworks.item

import dev.aaronhowser.mods.aaron.client.AaronClientUtil
import dev.aaronhowser.mods.critterworks.client.item.parry_platypus.ParryPlatypusItemRenderer
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.ShieldItem
import software.bernie.geckolib.animatable.GeoItem
import software.bernie.geckolib.animatable.client.GeoRenderProvider
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.animation.AnimatableManager
import software.bernie.geckolib.animation.AnimationController
import software.bernie.geckolib.animation.AnimationState
import software.bernie.geckolib.animation.PlayState
import software.bernie.geckolib.animation.RawAnimation
import software.bernie.geckolib.constant.DataTickets
import software.bernie.geckolib.util.GeckoLibUtil
import java.util.function.Consumer

class ParryPlatypusItem(properties: Properties) : ShieldItem(properties), GeoItem {

	private val animatableInstanceCache = GeckoLibUtil.createInstanceCache(this)

	override fun createGeoRenderer(consumer: Consumer<GeoRenderProvider>) {
		consumer.accept(object : GeoRenderProvider {
			private var renderer: ParryPlatypusItemRenderer? = null

			override fun getGeoItemRenderer(): BlockEntityWithoutLevelRenderer {
				var currentRenderer = renderer

				if (currentRenderer == null) {
					currentRenderer = ParryPlatypusItemRenderer()
					renderer = currentRenderer
				}

				return currentRenderer
			}
		})
	}

	override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar) {
		controllers.add(AnimationController(this, "held", 0, ::playHeldAnimation))
		controllers.add(AnimationController(this, "blocking", BLOCK_TRANSITION_TICKS, ::playBlockAnimation))
	}

	private fun playHeldAnimation(animationState: AnimationState<ParryPlatypusItem>): PlayState {
		val displayContext = animationState.getData(DataTickets.ITEM_RENDER_PERSPECTIVE)
		if (!isHandDisplayContext(displayContext)) return PlayState.STOP

		return animationState.setAndContinue(HELD_ANIMATION)
	}

	private fun isHandDisplayContext(displayContext: ItemDisplayContext?): Boolean {
		return displayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
			|| displayContext == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
			|| displayContext == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
			|| displayContext == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
	}

	private fun playBlockAnimation(animationState: AnimationState<ParryPlatypusItem>): PlayState {
		val stack = animationState.getData(DataTickets.ITEMSTACK) ?: return PlayState.STOP
		if (!isBeingUsed(stack)) return PlayState.STOP

		return animationState.setAndContinue(BLOCK_ANIMATION)
	}

	private fun isBeingUsed(stack: ItemStack): Boolean {
		val localPlayer = AaronClientUtil.localPlayer ?: return false

		for (player in localPlayer.level().players()) {
			if (player.isUsingItem && player.useItem === stack) return true
		}

		return false
	}

	override fun getAnimatableInstanceCache(): AnimatableInstanceCache = animatableInstanceCache

	companion object {
		val DEFAULT_PROPERTIES = {
			Properties()
				.durability(336)
		}

		private const val BLOCK_TRANSITION_TICKS = 3

		private val HELD_ANIMATION: RawAnimation =
			RawAnimation.begin()
				.thenLoop("animation.platypus.held")

		private val BLOCK_ANIMATION: RawAnimation =
			RawAnimation.begin()
				.thenPlayAndHold("animation.platypus.block")
	}
}