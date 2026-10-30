package dev.aaronhowser.mods.critterworks.item

import com.mojang.serialization.Codec
import dev.aaronhowser.mods.aaron.client.AaronClientUtil
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.getEquipmentSlot
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isClientSide
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isEntity
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isItem
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isNotTrue
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isTrue
import dev.aaronhowser.mods.aaron.serialization.AaronExtraStreamCodecs
import dev.aaronhowser.mods.critterworks.Critterworks
import dev.aaronhowser.mods.critterworks.client.item.snail_shield.SnailShieldItemRenderer
import dev.aaronhowser.mods.critterworks.datagen.tag.ModEntityTypeTagsProvider
import dev.aaronhowser.mods.critterworks.registry.ModAttachmentTypes
import dev.aaronhowser.mods.critterworks.registry.ModItems
import io.netty.buffer.ByteBuf
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer
import net.minecraft.network.codec.StreamCodec
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.util.StringRepresentable
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.NeutralMob
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.ai.memory.MemoryModuleType
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Rarity
import net.neoforged.neoforge.event.entity.EntityEvent
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent
import net.neoforged.neoforge.event.tick.PlayerTickEvent
import software.bernie.geckolib.animatable.GeoItem
import software.bernie.geckolib.animatable.client.GeoRenderProvider
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.animation.AnimatableManager
import software.bernie.geckolib.util.GeckoLibUtil
import java.util.function.Consumer

class SnailShieldItem(properties: Properties) : Item(properties), GeoItem {

	private val animatableInstanceCache = GeckoLibUtil.createInstanceCache(this)

	override fun createGeoRenderer(consumer: Consumer<GeoRenderProvider>) {
		consumer.accept(object : GeoRenderProvider {
			private var renderer: SnailShieldItemRenderer? = null

			override fun getGeoItemRenderer(): BlockEntityWithoutLevelRenderer {
				var currentRenderer = renderer

				if (currentRenderer == null) {
					currentRenderer = SnailShieldItemRenderer()
					renderer = currentRenderer
				}

				return currentRenderer
			}
		})
	}

	override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar) = Unit

	override fun getAnimatableInstanceCache(): AnimatableInstanceCache = animatableInstanceCache

	override fun isFoil(stack: ItemStack): Boolean {
		return AaronClientUtil.localPlayer?.shellState?.isShelled.isTrue()
	}

	override fun isValidRepairItem(stack: ItemStack, repairCandidate: ItemStack): Boolean {
		return repairCandidate.isItem(ModItems.SNAIL_SHELL_FRAGMENT)
	}

	companion object {

		val DEFAULT_PROPERTIES = {
			Properties()
				.durability(512)
				.rarity(Rarity.UNCOMMON)
		}

		private const val TICKS_TO_FULLY_SHELLED = 5 * 20
		private const val SHELLED_DIMENSIONS_SCALE = 0.4f

		val SHELLED_MOVEMENT_SPEED_MODIFIER = Critterworks.modResource("shelled_movement_speed")
		private val SHELLED_KNOCKBACK_RESISTANCE_MODIFIER = Critterworks.modResource("shelled_knockback_resistance")
		private val SHELLED_EXPLOSION_KNOCKBACK_RESISTANCE_MODIFIER = Critterworks.modResource("shelled_explosion_knockback_resistance")

		private var LivingEntity.shellStillTicks: Int
			get() = getData(ModAttachmentTypes.SNAIL_SHIELD_STATIONARY_TICKS)
			set(value) {
				setData(ModAttachmentTypes.SNAIL_SHIELD_STATIONARY_TICKS, value)
			}

		private var LivingEntity.shellState: SnailShieldState
			get() = getData(ModAttachmentTypes.SNAIL_SHIELD_STATE)
			set(value) {
				setData(ModAttachmentTypes.SNAIL_SHIELD_STATE, value)
			}

		fun handlePlayerTick(event: PlayerTickEvent.Post) {
			val player = event.entity
			if (player.isClientSide) {
				refreshClientDimensions(player)
				return
			}

			if (player !is ServerPlayer) return

			val isActivatingShell = player.isDiscrete && player.isHolding(ModItems.SNAIL_SHIELD.get())

			val oldState = player.shellState
			val newState = getNewShellState(player, isActivatingShell)

			if (newState == oldState) return

			player.shellState = newState
			updateAttributeModifiers(player, newState.isShelled)

			playTransitionSound(player, oldState, newState)

			if (newState == SnailShieldState.FULLY_SHELLED) {
				makeMobsForgetPlayer(player)
			}
		}

		private fun playTransitionSound(
			player: ServerPlayer,
			oldState: SnailShieldState,
			newState: SnailShieldState
		) {
			val sound = when {
				oldState.isShelled && !newState.isShelled -> SoundEvents.SHULKER_OPEN    // Deactivate
				!oldState.isShelled && newState.isShelled -> SoundEvents.SHULKER_CLOSE    // Activate
				oldState == SnailShieldState.PARTIALLY_SHELLED && newState == SnailShieldState.FULLY_SHELLED -> SoundEvents.ITEM_FRAME_ADD_ITEM // Hide head
				oldState == SnailShieldState.FULLY_SHELLED && newState == SnailShieldState.PARTIALLY_SHELLED -> SoundEvents.ITEM_FRAME_REMOVE_ITEM // Show head
				else -> return
			}

			player.level().playSound(
				null,
				player.blockPosition(),
				sound,
				SoundSource.PLAYERS
			)
		}

		private fun getNewShellState(
			player: ServerPlayer,
			isActivatingShell: Boolean
		): SnailShieldState {
			if (!isActivatingShell) {
				player.shellStillTicks = 0
				return SnailShieldState.UNSHELLED
			}

			val stationaryTicks = minOf(player.shellStillTicks + 1, TICKS_TO_FULLY_SHELLED)
			player.shellStillTicks = stationaryTicks

			return if (stationaryTicks >= TICKS_TO_FULLY_SHELLED) {
				SnailShieldState.FULLY_SHELLED
			} else {
				SnailShieldState.PARTIALLY_SHELLED
			}
		}

		private fun updateAttributeModifiers(player: Player, isShelled: Boolean) {
			val modifiers = mapOf(
				Attributes.MOVEMENT_SPEED to AttributeModifier(
					SHELLED_MOVEMENT_SPEED_MODIFIER,
					-0.3, // They're already slowed from sneaking
					AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
				),
				Attributes.KNOCKBACK_RESISTANCE to AttributeModifier(
					SHELLED_KNOCKBACK_RESISTANCE_MODIFIER,
					1.0,
					AttributeModifier.Operation.ADD_VALUE
				),
				Attributes.EXPLOSION_KNOCKBACK_RESISTANCE to AttributeModifier(
					SHELLED_EXPLOSION_KNOCKBACK_RESISTANCE_MODIFIER,
					1.0,
					AttributeModifier.Operation.ADD_VALUE
				)
			)

			for ((attribute, modifier) in modifiers) {
				val attributeInstance = player.getAttribute(attribute) ?: continue

				if (isShelled) {
					attributeInstance.addOrUpdateTransientModifier(modifier)
				} else {
					attributeInstance.removeModifier(modifier.id)
				}
			}
		}

		fun handleEntitySize(event: EntityEvent.Size) {
			val player = event.entity as? Player ?: return
			val shellState = player
				.getExistingDataOrNull(ModAttachmentTypes.SNAIL_SHIELD_STATE)

			if (shellState?.isShelled.isNotTrue()) return

			event.newSize = event.newSize.scale(SHELLED_DIMENSIONS_SCALE)
		}

		private fun refreshClientDimensions(player: Player) {
			val baseDimensions = player.getDimensions(player.pose)
			val expectedDimensions = if (player.shellState.isShelled) {
				baseDimensions.scale(SHELLED_DIMENSIONS_SCALE)
			} else {
				baseDimensions
			}

			if (player.bbWidth == expectedDimensions.width()
				&& player.bbHeight == expectedDimensions.height()
			) return

			player.refreshDimensions()
		}

		private fun makeMobsForgetPlayer(player: ServerPlayer) {
			val nearbyMobs = player.serverLevel().getEntitiesOfClass(
				Mob::class.java,
				player.boundingBox.inflate(100.0)
			)

			for (entity in nearbyMobs) {
				if (entity.type.isEntity(ModEntityTypeTagsProvider.CAN_TARGET_FULLY_SHELLED_PLAYERS)) continue

				if (entity.target === player) {
					entity.target = null
				}

				val attackTarget = entity.brain.getMemoryInternal(MemoryModuleType.ATTACK_TARGET)
				if (attackTarget != null && attackTarget.orElse(null) === player) {
					entity.brain.eraseMemory(MemoryModuleType.ATTACK_TARGET)
				}

				val angerTarget = entity.brain.getMemoryInternal(MemoryModuleType.ANGRY_AT)
				if (angerTarget != null && angerTarget.orElse(null) == player.uuid) {
					entity.brain.eraseMemory(MemoryModuleType.ANGRY_AT)
				}

				if (entity is NeutralMob && entity.persistentAngerTarget == player.uuid) {
					entity.stopBeingAngry()
				}
			}
		}

		fun handleIncomingDamage(event: LivingIncomingDamageEvent) {
			if (event.isCanceled) return

			val player = event.entity as? Player ?: return
			if (!player.shellState.isShelled) return

			event.isCanceled = true
			if (player.isClientSide) return

			player.level().playSound(
				null,
				player.blockPosition(),
				SoundEvents.SHULKER_HURT_CLOSED,
				SoundSource.PLAYERS
			)

			val shellHand = when {
				player.mainHandItem.isItem(ModItems.SNAIL_SHIELD) -> InteractionHand.MAIN_HAND
				player.offhandItem.isItem(ModItems.SNAIL_SHIELD) -> InteractionHand.OFF_HAND
				else -> return
			}

			val heldShell = player.getItemInHand(shellHand)
			heldShell.hurtAndBreak(1, player, shellHand.getEquipmentSlot())
		}

		fun handleTargetChange(event: LivingChangeTargetEvent) {
			val target = event.newAboutToBeSetTarget ?: return
			if (target.shellState != SnailShieldState.FULLY_SHELLED) return
			if (event.entity.type.isEntity(ModEntityTypeTagsProvider.CAN_TARGET_FULLY_SHELLED_PLAYERS)) return

			event.newAboutToBeSetTarget = null
		}

		fun handleClientMovement(player: ServerPlayer) {
			if (!player.isDiscrete || !player.isHolding(ModItems.SNAIL_SHIELD.get())) return

			player.shellStillTicks = 0

			if (player.shellState == SnailShieldState.FULLY_SHELLED) {
				player.shellState = SnailShieldState.PARTIALLY_SHELLED

				player.level().playSound(
					null,
					player.blockPosition(),
					SoundEvents.ITEM_FRAME_REMOVE_ITEM,
					SoundSource.PLAYERS,
					1f,
					1f
				)
			}
		}
	}

	enum class SnailShieldState(
		private val serializedName: String
	) : StringRepresentable {
		UNSHELLED("unshelled"),
		PARTIALLY_SHELLED("partially_shelled"),
		FULLY_SHELLED("fully_shelled");

		val isShelled: Boolean
			get() = this != UNSHELLED

		override fun getSerializedName(): String = serializedName

		companion object {
			val CODEC: Codec<SnailShieldState> =
				StringRepresentable.fromEnum { entries.toTypedArray() }

			val STREAM_CODEC: StreamCodec<ByteBuf, SnailShieldState> =
				AaronExtraStreamCodecs.enumStreamCodec(SnailShieldState::class.java)
		}
	}


}