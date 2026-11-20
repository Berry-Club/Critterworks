package dev.aaronhowser.mods.critterworks.item

import dev.aaronhowser.mods.aaron.misc.AaronExtensions.giveOrDropStack
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isClientSide
import dev.aaronhowser.mods.critterworks.entity.ItemCatchableEntity
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack

class CritterCatcherItem(properties: Properties) : Item(properties) {

	override fun interactLivingEntity(
		stack: ItemStack,
		player: Player,
		interactionTarget: LivingEntity,
		usedHand: InteractionHand
	): InteractionResult {
		if (interactionTarget !is ItemCatchableEntity || player.isClientSide) return InteractionResult.PASS
		val entityStack = interactionTarget.getAsItemstack() ?: return InteractionResult.PASS

		player.giveOrDropStack(entityStack)
		interactionTarget.discard()

		return InteractionResult.SUCCESS
	}

}