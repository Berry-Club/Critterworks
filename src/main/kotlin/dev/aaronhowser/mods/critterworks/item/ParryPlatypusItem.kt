package dev.aaronhowser.mods.critterworks.item

import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isDamageSource
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isItem
import dev.aaronhowser.mods.critterworks.client.item.parry_platypus.ParryPlatypusItemRenderer
import dev.aaronhowser.mods.critterworks.config.ServerConfig
import dev.aaronhowser.mods.critterworks.datagen.ModDamageTypes
import dev.aaronhowser.mods.critterworks.entity.CatchableMob
import dev.aaronhowser.mods.critterworks.entity.parry_platypus.ParryPlatypusEntity
import dev.aaronhowser.mods.critterworks.registry.ModDataComponents
import dev.aaronhowser.mods.critterworks.registry.ModEntityTypes
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.ShieldItem
import net.minecraft.world.level.Level
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent
import software.bernie.geckolib.animatable.GeoItem
import software.bernie.geckolib.animatable.SingletonGeoAnimatable
import software.bernie.geckolib.animatable.client.GeoRenderProvider
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.animation.*
import software.bernie.geckolib.constant.DataTickets
import software.bernie.geckolib.util.GeckoLibUtil
import java.util.function.Consumer

class ParryPlatypusItem(properties: Properties) : ShieldItem(properties), GeoItem {

	private val animatableInstanceCache = GeckoLibUtil.createInstanceCache(this)

	init {
		SingletonGeoAnimatable.registerSyncedAnimatable(this)
	}

	override fun isBarVisible(stack: ItemStack): Boolean {
		return CatchableMob.isHealthBarVisible(stack, ParryPlatypusEntity.MAX_HEALTH)
	}

	override fun getBarWidth(stack: ItemStack): Int {
		return CatchableMob.getHealthBarWidth(stack, ParryPlatypusEntity.MAX_HEALTH)
	}

	override fun getBarColor(stack: ItemStack): Int {
		return CatchableMob.getHealthBarColor(stack, ParryPlatypusEntity.MAX_HEALTH)
	}

	override fun isPerspectiveAware(): Boolean = true

	override fun createGeoRenderer(consumer: Consumer<GeoRenderProvider>) {
		consumer.accept(object : GeoRenderProvider {
			private val renderer by lazy { ParryPlatypusItemRenderer() }

			override fun getGeoItemRenderer(): BlockEntityWithoutLevelRenderer {
				return renderer
			}
		})
	}

	override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar) {
		controllers.add(AnimationController(this, "held", 0, ::playHeldAnimation))
		controllers.add(AnimationController(this, BLOCK_CONTROLLER, BLOCK_TRANSITION_TICKS, ::playBlockAnimation))
		controllers.add(
			AnimationController(this, PARRY_CONTROLLER, 0, ::playParryAnimation)
				.triggerableAnim(PARRY_TRIGGER, PARRY_ANIMATION)
		)
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

		if (level is ServerLevel) {
			GeoItem.getOrAssignId(stack, level)
		}

		if (entity is LivingEntity && entity.isUsingItem && entity.useItem === stack) return
		stack.platypusUseTicks = 0
	}

	override fun onEntityItemUpdate(stack: ItemStack, entity: ItemEntity): Boolean {
		val level = entity.level()
		if (level.isClientSide || entity.tickCount < SPAWN_DELAY_TICKS) return false

		val platypus = ModEntityTypes.PARRY_PLATYPUS.get().create(level) ?: return false
		platypus.moveTo(
			entity.x,
			entity.y,
			entity.z,
			entity.yRot,
			entity.xRot
		)

		platypus.setPlatypusStack(stack.copy())

		if (!level.addFreshEntity(platypus)) return false
		entity.discard()
		return true
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
		private const val SPAWN_DELAY_TICKS = 5 * 20
		private const val BLOCK_TRANSITION_TICKS = 3
		private const val BLOCK_CONTROLLER = "blocking"
		private const val PARRY_CONTROLLER = "parry"
		private const val PARRY_TRIGGER = "parry"

		val DEFAULT_PROPERTIES = {
			Properties()
				.stacksTo(1)
				.component(ModDataComponents.ENTITY_HEALTH, ParryPlatypusEntity.MAX_HEALTH)
				.component(ModDataComponents.PARRY_PLATYPUS_USE_TICKS, 0)
		}

		private val HELD_ANIMATION: RawAnimation =
			RawAnimation.begin()
				.thenLoop("animation.platypus.held")

		private val BLOCK_ANIMATION: RawAnimation =
			RawAnimation.begin()
				.thenPlayAndHold("animation.platypus.block")

		private val PARRY_ANIMATION: RawAnimation =
			RawAnimation.begin()
				.thenPlay("animation.platypus.parry")

		private var ItemStack.platypusUseTicks: Int
			get() = getOrDefault(ModDataComponents.PARRY_PLATYPUS_USE_TICKS, 0)
			set(value) {
				set(ModDataComponents.PARRY_PLATYPUS_USE_TICKS, value)
			}

		private fun playParryAnimation(animationState: AnimationState<ParryPlatypusItem>): PlayState {
			// Only exists to trigger the animation without canceling held/block concurrent animations
			return PlayState.STOP
		}

		private fun playHeldAnimation(animationState: AnimationState<ParryPlatypusItem>): PlayState {
			val isHeldInHand = when (animationState.getData(DataTickets.ITEM_RENDER_PERSPECTIVE)) {
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

			val isHeldInHand = when (animationState.getData(DataTickets.ITEM_RENDER_PERSPECTIVE)) {
				ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
				ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,
				ItemDisplayContext.THIRD_PERSON_LEFT_HAND,
				ItemDisplayContext.THIRD_PERSON_RIGHT_HAND -> true

				else -> false
			}

			return if (isHeldInHand)
				animationState.setAndContinue(BLOCK_ANIMATION)
			else
				PlayState.STOP
		}

		fun handleShieldBlock(event: LivingShieldBlockEvent) {
			if (!event.blocked) return

			val livingEntity = event.entity
			val level = livingEntity.level()
			if (level !is ServerLevel) return

			val stack = livingEntity.useItem
			val item = stack.item
			if (item !is ParryPlatypusItem) return

			reflectMeleeDamage(event, livingEntity, stack, level)

			item.triggerAnim<Any>(
				livingEntity,
				GeoItem.getOrAssignId(stack, level),
				PARRY_CONTROLLER,
				PARRY_TRIGGER
			)
		}

		private fun reflectMeleeDamage(
			event: LivingShieldBlockEvent,
			blockingEntity: LivingEntity,
			stack: ItemStack,
			level: ServerLevel
		) {
			val damageSource = event.damageSource
			if (!damageSource.isDirect || damageSource.isDamageSource(ModDamageTypes.PARRY_PLATYPUS)) return

			val attacker = damageSource.entity as? LivingEntity ?: return
			if (attacker === blockingEntity) return

			val isPerfectParry = stack.platypusUseTicks <= ServerConfig.CONFIG.platypusParryTiming.get()

			val reflectedDamageMultiplier = if (isPerfectParry) {
				ServerConfig.CONFIG.platypusPerfectReflectAmount.get()
			} else {
				ServerConfig.CONFIG.platypusRegularReflectAmount.get()
			}.toFloat()

			val reflectedDamage = event.blockedDamage * reflectedDamageMultiplier
			if (reflectedDamage > 0f) {
				attacker.hurt(
					blockingEntity.damageSources().source(ModDamageTypes.PARRY_PLATYPUS, blockingEntity),
					reflectedDamage
				)
			}

			if (!isPerfectParry) {
				val entityHealth = stack.getOrDefault(
					ModDataComponents.ENTITY_HEALTH,
					ParryPlatypusEntity.MAX_HEALTH
				)
				val remainingHealth = entityHealth - 1f

				if (remainingHealth <= 0f) {
					stack.shrink(1)
				} else {
					stack.set(ModDataComponents.ENTITY_HEALTH, remainingHealth)
				}

				return
			}

			val knockback = ServerConfig.CONFIG.platypusPerfectParryKnockback.get()
			val lookAngle = blockingEntity.lookAngle
			attacker.knockback(knockback, -lookAngle.x, -lookAngle.z)

			level.sendParticles(
				ParticleTypes.EXPLOSION,
				attacker.x,
				attacker.y + attacker.bbHeight / 2.0,
				attacker.z,
				3,
				0.15,
				0.15,
				0.15,
				0.0
			)
		}
	}
}