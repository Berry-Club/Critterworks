package dev.aaronhowser.mods.critterworks.handler.web.redstone.interaction

import dev.aaronhowser.mods.aaron.misc.AaronExtensions.getEquipmentSlot
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isItem
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.status
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.toComponent
import dev.aaronhowser.mods.critterworks.datagen.language.ModMessageLang
import dev.aaronhowser.mods.critterworks.event.custom.WebLineInteractionEvent
import dev.aaronhowser.mods.critterworks.event.custom.WebNodeInteractionEvent
import dev.aaronhowser.mods.critterworks.handler.web.WebLineType
import dev.aaronhowser.mods.critterworks.handler.web.redstone.RedstoneWebHandler
import dev.aaronhowser.mods.critterworks.handler.web.redstone.node.ArachnodePort
import dev.aaronhowser.mods.critterworks.item.WebstoneWeaverItem
import dev.aaronhowser.mods.critterworks.item.arachnode.ArachnodeItem
import dev.aaronhowser.mods.critterworks.registry.ModItems
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.gameevent.GameEvent
import net.neoforged.neoforge.common.Tags

object RedstoneWebInteractionHandler {

	fun handleNodeInteraction(event: WebNodeInteractionEvent) {
		if (event.webType != WebLineType.REDSTONE) return

		val player = event.player as? ServerPlayer ?: return
		val level = player.serverLevel()
		val redstoneWebHandler = RedstoneWebHandler.get(level)
		val selectedNode = redstoneWebHandler.getNode(event.node.uuid) ?: return
		if (selectedNode is ArachnodePort && event.itemStack.isEmpty) {
			val removedArachnode = redstoneWebHandler
				.removeArachnode(level, selectedNode.arachnodeUuid)
				?: return

			val returnedStack = removedArachnode.itemStack.copy()
			if (!player.addItem(returnedStack)) player.drop(returnedStack, false)
			level.gameEvent(player, GameEvent.BLOCK_DESTROY, selectedNode.position)
			return
		}

		if (!event.itemStack.isItem(ModItems.WEBSTONE_WEAVER)) return

		val webstoneWeaver = event.itemStack.item as? WebstoneWeaverItem ?: return
		webstoneWeaver.handleNodeSelection(level, player, event.itemStack, selectedNode)
	}

	fun handleLineInteraction(event: WebLineInteractionEvent) {
		if (event.webType != WebLineType.REDSTONE) return

		val player = event.player as? ServerPlayer ?: return
		val level = player.serverLevel()
		val redstoneWebHandler = RedstoneWebHandler.get(level)
		if (redstoneWebHandler.getLine(event.lineUuid) == null) return

		when {
			event.itemStack.item is ArachnodeItem -> {
				val placed = redstoneWebHandler.insertArachnodeOnLine(
					level,
					event.lineUuid,
					event.itemStack,
					event.target.position,
					player.eyePosition
				)
				if (!placed) return

				event.itemStack.consume(1, player)
				level.gameEvent(player, GameEvent.BLOCK_PLACE, event.target.position)
			}

			event.itemStack.isItem(Tags.Items.TOOLS_SHEAR) -> {
				if (redstoneWebHandler.removeLine(level, event.lineUuid) == null) return
				event.itemStack.hurtAndBreak(1, player, event.hand.getEquipmentSlot())
				player.status(ModMessageLang.LINE_REMOVED_MESSAGE.toComponent())
				level.gameEvent(player, GameEvent.SHEAR, event.target.position)
			}

			event.itemStack.isItem(ModItems.WEBSTONE_WEAVER) -> {
				val webstoneWeaver = event.itemStack.item as? WebstoneWeaverItem ?: return
				webstoneWeaver.handleNodeSelection(level, player, event.itemStack, event.target)
			}
		}
	}
}