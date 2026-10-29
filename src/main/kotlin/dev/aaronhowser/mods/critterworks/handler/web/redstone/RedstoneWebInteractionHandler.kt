package dev.aaronhowser.mods.critterworks.handler.web.redstone

import dev.aaronhowser.mods.aaron.misc.AaronExtensions.getEquipmentSlot
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isItem
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.status
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.toComponent
import dev.aaronhowser.mods.critterworks.datagen.language.ModMessageLang
import dev.aaronhowser.mods.critterworks.event.custom.WebLineInteractionEvent
import dev.aaronhowser.mods.critterworks.event.custom.WebNodeInteractionEvent
import dev.aaronhowser.mods.critterworks.handler.web.WebLineType
import dev.aaronhowser.mods.critterworks.item.WebstoneWeaverItem
import dev.aaronhowser.mods.critterworks.registry.ModItems
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.gameevent.GameEvent
import net.neoforged.neoforge.common.Tags

object RedstoneWebInteractionHandler {

	fun handleNodeInteraction(event: WebNodeInteractionEvent) {
		if (event.webType != WebLineType.REDSTONE) return

		val player = event.player as? ServerPlayer ?: return
		if (!event.itemStack.isItem(ModItems.WEBSTONE_WEAVER)) return

		val level = player.serverLevel()
		val selectedNode = RedstoneWebHandler.get(level).getNode(event.node.uuid) ?: return
		val webstoneWeaver = event.itemStack.item as? WebstoneWeaverItem ?: return
		webstoneWeaver.handleNodeSelection(level, player, event.itemStack, selectedNode)
	}

	fun handleLineInteraction(event: WebLineInteractionEvent) {
		if (event.webType != WebLineType.REDSTONE) return

		val player = event.player as? ServerPlayer ?: return
		val level = player.serverLevel()
		if (RedstoneWebHandler.get(level).getLine(event.lineUuid) == null) return

		when {
			event.itemStack.isItem(Tags.Items.TOOLS_SHEAR) -> {
				if (RedstoneWebHandler.get(level).removeLine(level, event.lineUuid) == null) return
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