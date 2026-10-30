package dev.aaronhowser.mods.critterworks.client.web

import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isItem
import dev.aaronhowser.mods.critterworks.datagen.tag.ModItemTagsProvider
import dev.aaronhowser.mods.critterworks.event.custom.WebLineInteractionEvent
import dev.aaronhowser.mods.critterworks.event.custom.WebNodeInteractionEvent
import dev.aaronhowser.mods.critterworks.handler.web.common.interaction.WebInteractionHandler
import dev.aaronhowser.mods.critterworks.handler.web.WebLineType
import dev.aaronhowser.mods.critterworks.handler.web.common.interaction.WebTargeting
import dev.aaronhowser.mods.critterworks.handler.web.common.interaction.TargetedWebNode
import dev.aaronhowser.mods.critterworks.handler.web.common.node.WebBlockAnchor
import dev.aaronhowser.mods.critterworks.handler.web.common.node.WebLineAnchor
import dev.aaronhowser.mods.critterworks.handler.web.redstone.node.ArachnodePort
import dev.aaronhowser.mods.critterworks.packet.client_to_server.WebLineInteractPacket
import net.minecraft.client.Minecraft
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import net.neoforged.neoforge.client.event.InputEvent
import net.neoforged.neoforge.common.NeoForge

object ClientWebLineInteractionHandler {

	fun handleInteraction(event: InputEvent.InteractionKeyMappingTriggered) {
		if (!event.isUseItem) return
		interactWithTarget(event)
	}

	fun getHoveredAnchor(
		player: Player,
		eyePosition: Vec3,
		viewVector: Vec3
	): TargetedWebNode? {
		val interactionHand = getInteractionHand(player) ?: InteractionHand.MAIN_HAND
		val target = getTargetedNode(player, eyePosition, viewVector, interactionHand) ?: return null
		val heldStack = player.getItemInHand(interactionHand)
		if (WebInteractionHandler.canInteractWithType(heldStack, target.first)) return target.second
		if (!targetsInstalledWebPort(target) && !targetsArachnode(target, heldStack)) return null

		return target.second
	}

	private fun interactWithTarget(event: InputEvent.InteractionKeyMappingTriggered) {
		val player = Minecraft.getInstance().player ?: return
		val interactionHand = getInteractionHand(player) ?: event.hand
		val target = getTargetedNode(
			player,
			player.eyePosition,
			player.lookAngle,
			interactionHand
		) ?: return
		val targetedNode = target.second
		val heldStack = player.getItemInHand(interactionHand)
		if (!WebInteractionHandler.canInteractWithType(heldStack, target.first)
			&& !targetsInstalledWebPort(target)
			&& !targetsArachnode(target, heldStack)
		) return

		val interactionEvent = if (targetedNode.lineUuid == null) {
			WebNodeInteractionEvent(player, target.first, targetedNode.node, heldStack, interactionHand)
		} else {
			WebLineInteractionEvent(
				player,
				target.first,
				targetedNode.lineUuid,
				targetedNode.node as WebLineAnchor,
				heldStack,
				interactionHand
			)
		}

		NeoForge.EVENT_BUS.post(interactionEvent)
		if (event.hand == interactionHand && !interactionEvent.isCanceled) {
			sendInteraction(target, interactionHand)
		}

		event.isCanceled = true
	}

	private fun sendInteraction(
		target: Pair<WebLineType, TargetedWebNode>,
		hand: InteractionHand
	) {
		val targetedNode = target.second
		val targetsNode = targetedNode.lineUuid == null
		val targetUuid = targetedNode.lineUuid ?: targetedNode.node.uuid

		WebLineInteractPacket(
			target.first,
			targetUuid,
			targetsNode,
			targetedNode.node.position,
			hand
		).messageServer()
	}

	private fun getInteractionHand(player: Player): InteractionHand? {
		if (canInteractWithWeb(player.mainHandItem)) {
			return InteractionHand.MAIN_HAND
		}
		if (canInteractWithWeb(player.offhandItem)) {
			return InteractionHand.OFF_HAND
		}
		return null
	}

	private fun getTargetedNode(
		player: Player,
		eyePosition: Vec3,
		viewVector: Vec3,
		interactionHand: InteractionHand
	): Pair<WebLineType, TargetedWebNode>? {
		val lookEnd = eyePosition.add(viewVector.scale(player.blockInteractionRange()))
		val heldStack = player.getItemInHand(interactionHand)
		val canInteractWithItemWeb = WebInteractionHandler.canInteractWithType(heldStack, WebLineType.ITEM)
		val canInteractWithRedstoneWeb = WebInteractionHandler.canInteractWithType(heldStack, WebLineType.REDSTONE)

		if (!canInteractWithRedstoneWeb || canInteractWithItemWeb) {
			val installedWebPort = WebTargeting.getTargetedNode(
				ClientWebLines.getLines(WebLineType.ITEM),
				eyePosition,
				lookEnd,
				snapToExistingNode = true,
				requireExistingNode = true
			)
			val target = if (installedWebPort == null) {
				null
			} else {
				Pair(WebLineType.ITEM, installedWebPort)
			}
			if (target != null && targetsInstalledWebPort(target)) {
				if (isBehindCurrentHit(target.second, eyePosition)) return null
				return target
			}
		}

		if (heldStack.isEmpty) {
			val targetedArachnodePort = WebTargeting.getTargetedNode(
				ClientWebLines.getLines(WebLineType.REDSTONE),
				eyePosition,
				lookEnd,
				snapToExistingNode = true,
				requireExistingNode = true
			)
			val target = if (targetedArachnodePort == null) {
				null
			} else {
				Pair(WebLineType.REDSTONE, targetedArachnodePort)
			}
			if (target != null && targetsArachnode(target, heldStack)) {
				if (isBehindCurrentHit(target.second, eyePosition)) return null
				return target
			}
		}

		val webType = when {
			canInteractWithItemWeb && !canInteractWithRedstoneWeb -> WebLineType.ITEM
			canInteractWithRedstoneWeb && !canInteractWithItemWeb -> WebLineType.REDSTONE
			else -> null
		}
		if (webType != null) {
			val target = WebTargeting.getTargetedNode(
				ClientWebLines.getLines(webType),
				eyePosition,
				lookEnd,
				heldStack.isItem(ModItemTagsProvider.SNAP_TO_NODE),
				heldStack.isItem(ModItemTagsProvider.REQUIRES_EXISTING_NODE)
			) ?: return null
			return Pair(webType, target)
		}

		val target = WebTargeting.getTargetedNode(
			ClientWebLines.getLines(),
			eyePosition,
			lookEnd,
			heldStack.isItem(ModItemTagsProvider.SNAP_TO_NODE),
			heldStack.isItem(ModItemTagsProvider.REQUIRES_EXISTING_NODE)
		) ?: return null
		val targetType = ClientWebLines.getType(target) ?: return null
		return Pair(targetType, target)
	}

	private fun canInteractWithWeb(itemStack: ItemStack): Boolean {
		return WebInteractionHandler.canInteractWithType(itemStack, WebLineType.ITEM)
			|| WebInteractionHandler.canInteractWithType(itemStack, WebLineType.REDSTONE)
	}

	private fun targetsInstalledWebPort(target: Pair<WebLineType, TargetedWebNode>): Boolean {
		if (target.first != WebLineType.ITEM) return false
		if (target.second.lineUuid != null) return false
		val blockAnchor = target.second.node as? WebBlockAnchor ?: return false
		return !ClientItemWebPorts.get(blockAnchor.uuid).isEmpty
	}

	private fun targetsArachnode(
		target: Pair<WebLineType, TargetedWebNode>,
		heldStack: ItemStack
	): Boolean {
		if (!heldStack.isEmpty) return false
		if (target.first != WebLineType.REDSTONE) return false
		if (target.second.lineUuid != null) return false
		return target.second.node is ArachnodePort
	}

	private fun isBehindCurrentHit(targetedNode: TargetedWebNode, eyePosition: Vec3): Boolean {
		val hitResult = Minecraft.getInstance().hitResult ?: return false
		if (hitResult.type == HitResult.Type.MISS) return false

		val hitDistanceSquared = eyePosition.distanceToSqr(hitResult.location)
		val webDistanceSquared = eyePosition.distanceToSqr(targetedNode.node.position)
		return hitDistanceSquared < webDistanceSquared
	}
}