package dev.aaronhowser.mods.critterworks.item

import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isItem
import dev.aaronhowser.mods.critterworks.client.item.parry_platypus.ParryPlatypusItemRenderer
import dev.aaronhowser.mods.critterworks.registry.ModDataComponents
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.ShieldItem
import net.minecraft.world.item.UseAnim
import net.minecraft.world.level.Level
import software.bernie.geckolib.animatable.GeoItem
import software.bernie.geckolib.animatable.client.GeoRenderProvider
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.animation.*
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

		val isHeldInHand = when (displayContext) {
			ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
			ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,
			ItemDisplayContext.THIRD_PERSON_LEFT_HAND,
			ItemDisplayContext.THIRD_PERSON_RIGHT_HAND -> true

			else -> false
		}

		return if (isHeldInHand)
			animationState.setAndContinue(HELD_ANIMATION)
		else
			PlayState.STOP
	}

	private fun playBlockAnimation(animationState: AnimationState<ParryPlatypusItem>): PlayState {
		val stack = animationState.getData(DataTickets.ITEMSTACK) ?: return PlayState.STOP
		if (stack.platypusUseTicks == 0) return PlayState.STOP

		return animationState.setAndContinue(BLOCK_ANIMATION)
	}

	override fun use(
		level: Level,
		player: Player,
		hand: InteractionHand
	): InteractionResultHolder<ItemStack> {
		val stack = player.getItemInHand(hand)
		if (stack.isItem(this)) {
			stack.platypusUseTicks = 0
		}

		return super.use(level, player, hand)
	}

	override fun onUseTick(
		level: Level,
		livingEntity: LivingEntity,
		stack: ItemStack,
		remainingUseDuration: Int
	) {
		super.onUseTick(level, livingEntity, stack, remainingUseDuration)
		stack.platypusUseTicks++
	}

	override fun inventoryTick(
		stack: ItemStack,
		level: Level,
		entity: Entity,
		slotId: Int,
		isSelected: Boolean
	) {
		super.inventoryTick(stack, level, entity, slotId, isSelected)

		if (entity is LivingEntity && entity.isUsingItem && entity.useItem === stack) return
		stack.platypusUseTicks = 0
	}

	override fun releaseUsing(
		stack: ItemStack,
		level: Level,
		livingEntity: LivingEntity,
		timeCharged: Int
	) {
		super.releaseUsing(stack, level, livingEntity, timeCharged)
		stack.platypusUseTicks = 0
	}

	override fun shouldCauseReequipAnimation(oldStack: ItemStack, newStack: ItemStack, slotChanged: Boolean): Boolean {
		return slotChanged || oldStack.item != newStack.item
	}

	override fun getAnimatableInstanceCache(): AnimatableInstanceCache = animatableInstanceCache

	companion object {
		private const val BLOCK_TRANSITION_TICKS = 3

		val DEFAULT_PROPERTIES = {
			Properties()
				.durability(336)
				.component(ModDataComponents.PARRY_PLATYPUS_USE_TICKS, 0)
		}

		private val HELD_ANIMATION: RawAnimation =
			RawAnimation.begin()
				.thenLoop("animation.platypus.held")

		private val BLOCK_ANIMATION: RawAnimation =
			RawAnimation.begin()
				.thenPlayAndHold("animation.platypus.block")

		private var ItemStack.platypusUseTicks: Int
			get() = getOrDefault(ModDataComponents.PARRY_PLATYPUS_USE_TICKS, 0)
			set(value) {
				set(ModDataComponents.PARRY_PLATYPUS_USE_TICKS, value)
			}
	}
}