package dev.aaronhowser.mods.critterworks.handler.web.redstone.interaction

import dev.aaronhowser.mods.critterworks.handler.web.common.node.WebLineAnchor
import dev.aaronhowser.mods.critterworks.handler.web.redstone.RedstoneWebHandler
import dev.aaronhowser.mods.critterworks.handler.web.redstone.RedstoneWebLine
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.ItemStack

class RedstoneWebLineInteractionContext(
	val level: ServerLevel,
	val player: ServerPlayer,
	val itemStack: ItemStack,
	val hand: InteractionHand,
	val redstoneWebHandler: RedstoneWebHandler,
	val line: RedstoneWebLine,
	val target: WebLineAnchor
)