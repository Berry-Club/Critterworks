package dev.aaronhowser.mods.critterworks.event.custom

import dev.aaronhowser.mods.critterworks.handler.web.node.WebNode
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.neoforged.bus.api.Event
import net.neoforged.bus.api.ICancellableEvent

class WebNodeInteractionEvent(
	val player: Player,
	val node: WebNode,
	val itemStack: ItemStack,
	val hand: InteractionHand
) : Event(), ICancellableEvent
