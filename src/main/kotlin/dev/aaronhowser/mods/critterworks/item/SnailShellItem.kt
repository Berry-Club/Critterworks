package dev.aaronhowser.mods.critterworks.item

import dev.aaronhowser.mods.aaron.client.AaronClientUtil
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isEntity
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isItem
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isServerSide
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isTrue
import dev.aaronhowser.mods.critterworks.Critterworks
import dev.aaronhowser.mods.critterworks.datagen.tag.ModEntityTypeTagsProvider
import dev.aaronhowser.mods.critterworks.registry.ModAttachmentTypes
import dev.aaronhowser.mods.critterworks.registry.ModItems
import io.netty.buffer.ByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.NeutralMob
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.ai.memory.MemoryModuleType
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent
import net.neoforged.neoforge.event.tick.PlayerTickEvent

class SnailShellItem(properties: Properties) : Item(properties) {

	override fun isFoil(stack: ItemStack): Boolean {
		return AaronClientUtil.localPlayer?.shellState?.isShelled.isTrue()
	}

	override fun isValidRepairItem(stack: ItemStack, repairCandidate: ItemStack): Boolean {
		return repairCandidate.isItem(ModItems.SNAIL_SHELL_FRAGMENT)
	}

	companion object {

		val DEFAULT_PROPERTIES = {
			Properties()
				.durability(256)
		}

		private const val TICKS_TO_FULLY_SHELLED = 5 * 20

		private val SHELLED_SCALE_MODIFIER = Critterworks.modResource("shelled_scale")
		private val SHELLED_MOVEMENT_SPEED_MODIFIER = Critterworks.modResource("shelled_movement_speed")
		private val SHELLED_KNOCKBACK_RESISTANCE_MODIFIER = Critterworks.modResource("shelled_knockback_resistance")
		private val SHELLED_EXPLOSION_KNOCKBACK_RESISTANCE_MODIFIER = Critterworks.modResource("shelled_explosion_knockback_resistance")

		private var LivingEntity.shellStillTicks: Int
			get() = getData(ModAttachmentTypes.SNAIL_SHELL_STATIONARY_TICKS)
			set(value) {
				setData(ModAttachmentTypes.SNAIL_SHELL_STATIONARY_TICKS, value)
			}

		private var LivingEntity.shellState: SnailShellState
			get() = getData(ModAttachmentTypes.SNAIL_SHELL_STATE)
			set(value) {
				setData(ModAttachmentTypes.SNAIL_SHELL_STATE, value)
			}

		fun handlePlayerTick(event: PlayerTickEvent.Post) {
			val player = event.entity
			if (player !is ServerPlayer) return

			val isActivatingShell = player.isDiscrete && player.isHolding(ModItems.SNAIL_SHELL.get())

			val previousState = player.shellState
			val newState = getNewShellState(player, isActivatingShell)

			if (newState == previousState) return

			player.shellState = newState
			updateAttributeModifiers(player, newState.isShelled)

			if (previousState.isShelled != newState.isShelled) {
				player.level().playSound(
					null,
					player.blockPosition(),
					if (newState.isShelled) SoundEvents.SHULKER_CLOSE else SoundEvents.SHULKER_OPEN,
					SoundSource.PLAYERS,
					1f,
					1f
				)
			}

			if (newState == SnailShellState.FULLY_SHELLED) {
				makeMobsForgetPlayer(player)
			}
		}

		private fun getNewShellState(
			player: ServerPlayer,
			isActivatingShell: Boolean
		): SnailShellState {
			if (!isActivatingShell) {
				player.shellStillTicks = 0
				return SnailShellState.UNSHELLED
			}

			val stationaryTicks = minOf(player.shellStillTicks + 1, TICKS_TO_FULLY_SHELLED)
			player.shellStillTicks = stationaryTicks

			return if (stationaryTicks >= TICKS_TO_FULLY_SHELLED) {
				SnailShellState.FULLY_SHELLED
			} else {
				SnailShellState.PARTIALLY_SHELLED
			}
		}

		private fun updateAttributeModifiers(player: Player, isShelled: Boolean) {
			val modifiers = mapOf(
				Attributes.SCALE to AttributeModifier(
					SHELLED_SCALE_MODIFIER,
					-0.6,
					AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
				),
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

			val entity = event.entity
			val shellState = entity.shellState
			if (!shellState.isShelled) return

			event.isCanceled = true

			if (entity.isServerSide) {
				entity.level().playSound(
					null,
					entity.blockPosition(),
					SoundEvents.SHULKER_HURT_CLOSED,
					SoundSource.PLAYERS
				)
			}
		}

		fun handleTargetChange(event: LivingChangeTargetEvent) {
			val target = event.newAboutToBeSetTarget ?: return
			if (target.shellState != SnailShellState.FULLY_SHELLED) return
			if (event.entity.type.isEntity(ModEntityTypeTagsProvider.CAN_TARGET_FULLY_SHELLED_PLAYERS)) return

			event.newAboutToBeSetTarget = null
		}

		fun handleClientMovement(player: ServerPlayer) {
			if (!player.isDiscrete || !player.isHolding(ModItems.SNAIL_SHELL.get())) return

			player.shellStillTicks = 0

			if (player.shellState == SnailShellState.FULLY_SHELLED) {
				player.shellState = SnailShellState.PARTIALLY_SHELLED
			}
		}
	}

	enum class SnailShellState {
		UNSHELLED,
		PARTIALLY_SHELLED,
		FULLY_SHELLED;

		val isShelled: Boolean
			get() = this != UNSHELLED

		companion object {
			val STREAM_CODEC: StreamCodec<ByteBuf, SnailShellState> =
				ByteBufCodecs.idMapper(
					SnailShellState::fromOrdinal,
					SnailShellState::ordinal
				)

			private fun fromOrdinal(ordinal: Int): SnailShellState {
				return entries.getOrElse(ordinal) { UNSHELLED }
			}
		}
	}


}