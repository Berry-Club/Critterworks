package dev.aaronhowser.mods.critterworks.event.custom

import dev.aaronhowser.mods.critterworks.handler.web.node.WebLineAnchor
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.neoforged.bus.api.Event
import net.neoforged.bus.api.ICancellableEvent
import java.util.UUID

class WebLineInteractionEvent(
	val player: Player,
	val lineUuid: UUID,
	val target: WebLineAnchor,
	val itemStack: ItemStack,
	val hand: InteractionHand
) : Event(), ICancellableEvent
