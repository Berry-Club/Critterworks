package dev.aaronhowser.mods.critterworks.item

import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isDamageSource
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isItem
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isServerSide
import dev.aaronhowser.mods.critterworks.Critterworks
import dev.aaronhowser.mods.critterworks.config.ServerConfig
import dev.aaronhowser.mods.critterworks.registry.ModAttachmentTypes
import dev.aaronhowser.mods.critterworks.registry.ModItems
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.tags.DamageTypeTags
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.item.Item
import net.minecraft.world.phys.Vec3
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent
import net.neoforged.neoforge.event.tick.PlayerTickEvent

class SnailShellItem(properties: Properties) : Item(properties) {

	companion object {
		val SHELLED_SCALE_MODIFIER = Critterworks.modResource("shelled_scale")

		fun handlePlayerTick(event: PlayerTickEvent.Post) {
			val player = event.entity

			val isShellProtecting = player.isDiscrete
				&& !player.abilities.flying
				&& !player.cooldowns.isOnCooldown(ModItems.SNAIL_SHELL.get())
				&& player.inventory.contains { it.isItem(ModItems.SNAIL_SHELL) }

			if (isShellProtecting && ServerConfig.CONFIG.snailShellDisablesAllMovement.get()) {
				player.deltaMovement = Vec3.ZERO
			}

			if (player.isServerSide) {
				val wasShellProtecting = player.getData(ModAttachmentTypes.IS_SHELL_ACTIVE)

				if (isShellProtecting != wasShellProtecting) {
					player.setData(ModAttachmentTypes.IS_SHELL_ACTIVE, isShellProtecting)

					player.level().playSound(
						null,
						player.blockPosition(),
						SoundEvents.ARMOR_EQUIP_TURTLE.value(),
						SoundSource.PLAYERS,
						1f,
						if (isShellProtecting) 1f else 1.2f
					)
				}

				//TODO: Also makes the snail model smaller
				val scaleAttribute = player.getAttribute(Attributes.SCALE)
				if (isShellProtecting) {
					val modifier = AttributeModifier(
						SHELLED_SCALE_MODIFIER,
						-0.6,
						AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
					)

					scaleAttribute?.addOrUpdateTransientModifier(modifier)
				} else {
					scaleAttribute?.removeModifier(SHELLED_SCALE_MODIFIER)
				}
			}
		}

		fun handleIncomingDamage(event: LivingIncomingDamageEvent) {
			if (event.isCanceled) return

			val source = event.source
			if (source.isDamageSource(DamageTypeTags.NO_IMPACT)) return

			val entity = event.entity
			if (!entity.getData(ModAttachmentTypes.IS_SHELL_ACTIVE)) return

			event.isCanceled = true

			if (entity.isServerSide) {
				entity.level().playSound(
					null,
					entity.blockPosition(),
					SoundEvents.SHIELD_BLOCK,
					SoundSource.PLAYERS
				)
			}
		}
	}

}