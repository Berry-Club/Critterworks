package dev.aaronhowser.mods.critterworks.item

import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isClientSide
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isItem
import dev.aaronhowser.mods.critterworks.registry.ModAttachmentTypes
import dev.aaronhowser.mods.critterworks.registry.ModItems
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.item.Item
import net.neoforged.neoforge.event.tick.PlayerTickEvent

class SnailShellItem(properties: Properties) : Item(properties) {

	companion object {
		fun handlePlayerTick(event: PlayerTickEvent.Post) {
			val player = event.entity
			if (player.isClientSide) return

			val isShellProtecting = player.isDiscrete
				&& !player.cooldowns.isOnCooldown(ModItems.SNAIL_SHELL.get())
				&& player.inventory.contains { it.isItem(ModItems.SNAIL_SHELL) }

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
		}
	}

}