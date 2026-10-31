package dev.aaronhowser.mods.critterworks.handler.web.common.interaction

import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isItem
import dev.aaronhowser.mods.critterworks.datagen.tag.ModItemTagsProvider
import dev.aaronhowser.mods.critterworks.event.custom.WebLineInteractionEvent
import dev.aaronhowser.mods.critterworks.event.custom.WebNodeInteractionEvent
import dev.aaronhowser.mods.critterworks.handler.web.WebLineType
import dev.aaronhowser.mods.critterworks.handler.web.common.line.WebLine
import dev.aaronhowser.mods.critterworks.handler.web.common.node.WebLineAnchor
import dev.aaronhowser.mods.critterworks.handler.web.common.node.WebNode
import dev.aaronhowser.mods.critterworks.handler.web.item.ItemWebHandler
import dev.aaronhowser.mods.critterworks.handler.web.redstone.RedstoneWebHandler
import dev.aaronhowser.mods.critterworks.item.arachnode.ArachnodeItem
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3
import net.neoforged.neoforge.common.NeoForge
import java.util.*

object WebInteractionHandler {

	fun interact(
		player: ServerPlayer,
		webType: WebLineType,
		targetUuid: UUID,
		targetsNode: Boolean,
		requestedPosition: Vec3,
		hand: InteractionHand
	) {
		val itemStack = player.getItemInHand(hand)

		if (targetsNode) {
			interactNode(player, webType, targetUuid, requestedPosition, itemStack, hand)
		} else {
			if (!canInteractWithType(itemStack, webType)) return
			interactLine(player, webType, targetUuid, requestedPosition, itemStack, hand)
		}
	}

	private fun interactNode(
		player: ServerPlayer,
		webType: WebLineType,
		targetUuid: UUID,
		requestedPosition: Vec3,
		itemStack: ItemStack,
		hand: InteractionHand
	) {
		val target = getNode(player.serverLevel(), webType, targetUuid) ?: return
		if (!isTargetingNode(player, target)) return
		if (!isWithinPositionTolerance(target.position, requestedPosition)) return

		NeoForge.EVENT_BUS.post(WebNodeInteractionEvent(player, webType, target, itemStack, hand))
	}

	private fun interactLine(
		player: ServerPlayer,
		webType: WebLineType,
		lineUuid: UUID,
		requestedPosition: Vec3,
		itemStack: ItemStack,
		hand: InteractionHand
	) {
		val line = getLine(player.serverLevel(), webType, lineUuid) ?: return
		val eyePosition = player.eyePosition
		val lookEnd = eyePosition.add(player.lookAngle.scale(player.blockInteractionRange()))
		val targetedNode = WebTargeting.getTargetedNode(
			listOf(line),
			eyePosition,
			lookEnd,
			itemStack.isItem(ModItemTagsProvider.SNAP_TO_NODE),
			itemStack.isItem(ModItemTagsProvider.REQUIRES_EXISTING_NODE)
		) ?: return

		val lineAnchor = targetedNode.node as? WebLineAnchor ?: return

		if (!isWithinPositionTolerance(lineAnchor.position, requestedPosition)) return

		NeoForge.EVENT_BUS.post(
			WebLineInteractionEvent(player, webType, lineUuid, lineAnchor, itemStack, hand)
		)
	}

	fun canInteractWithType(itemStack: ItemStack, webType: WebLineType): Boolean {
		return when (webType) {
			WebLineType.ITEM -> itemStack.isItem(ModItemTagsProvider.ITEM_WEB_LINE_INTERACTABLE)
			WebLineType.REDSTONE -> itemStack.item is ArachnodeItem
				|| itemStack.isItem(ModItemTagsProvider.REDSTONE_WEB_LINE_INTERACTABLE)
		}
	}

	private fun getNode(level: ServerLevel, webType: WebLineType, uuid: UUID): WebNode? {
		return when (webType) {
			WebLineType.ITEM -> ItemWebHandler.get(level).getNode(uuid)
			WebLineType.REDSTONE -> RedstoneWebHandler.get(level).getNode(uuid)
		}
	}

	private fun getLine(level: ServerLevel, webType: WebLineType, uuid: UUID): WebLine? {
		return when (webType) {
			WebLineType.ITEM -> ItemWebHandler.get(level).getLine(uuid)
			WebLineType.REDSTONE -> RedstoneWebHandler.get(level).getLine(uuid)
		}
	}

	private fun isTargetingNode(player: ServerPlayer, node: WebNode): Boolean {
		val lookStart = player.eyePosition
		val lookEnd = lookStart.add(player.lookAngle.scale(player.blockInteractionRange()))
		return WebTargeting.isTargetingNode(lookStart, lookEnd, node)
	}

	private fun isWithinPositionTolerance(expected: Vec3, actual: Vec3): Boolean {
		return expected.distanceToSqr(actual) <= POSITION_TOLERANCE * POSITION_TOLERANCE
	}

	private const val POSITION_TOLERANCE = 0.1
}