package dev.aaronhowser.mods.critterworks.handler.web.line

import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isItem
import dev.aaronhowser.mods.critterworks.datagen.tag.ModItemTagsProvider
import dev.aaronhowser.mods.critterworks.event.custom.WebLineInteractionEvent
import dev.aaronhowser.mods.critterworks.event.custom.WebNodeInteractionEvent
import dev.aaronhowser.mods.critterworks.handler.web.TargetedWebNode
import dev.aaronhowser.mods.critterworks.handler.web.WebLineInteractionHandler
import dev.aaronhowser.mods.critterworks.handler.web.node.WebBlockAnchor
import dev.aaronhowser.mods.critterworks.handler.web.node.WebLineAnchor
import dev.aaronhowser.mods.critterworks.packet.client_to_server.WebLineInteractPacket
import dev.aaronhowser.mods.critterworks.registry.ModItems
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
		val targetedNode = getTargetedNode(
			player,
			eyePosition,
			viewVector,
			interactionHand
		) ?: return null

		val heldStack = player.getItemInHand(interactionHand)
		if (heldStack.isItem(ModItemTagsProvider.WEB_LINE_INTERACTABLE)) {
			return targetedNode
		}

		if (!targetsInstalledWebPort(targetedNode)) return null

		return targetedNode
	}

	private fun interactWithTarget(event: InputEvent.InteractionKeyMappingTriggered) {
		val player = Minecraft.getInstance().player ?: return
		val interactionHand = getInteractionHand(player) ?: event.hand

		val targetedNode = getTargetedNode(
			player,
			player.eyePosition,
			player.lookAngle,
			interactionHand
		) ?: return

		val heldStack = player.getItemInHand(interactionHand)
		if (!heldStack.isItem(ModItemTagsProvider.WEB_LINE_INTERACTABLE)
			&& !targetsInstalledWebPort(targetedNode)
		) {
			return
		}

		val interactionEvent = if (targetedNode.lineUuid == null) {
			WebNodeInteractionEvent(player, targetedNode.node, heldStack, interactionHand)
		} else {
			WebLineInteractionEvent(
				player,
				targetedNode.lineUuid,
				targetedNode.node as WebLineAnchor,
				heldStack,
				interactionHand
			)
		}

		NeoForge.EVENT_BUS.post(interactionEvent)

		if (event.hand == interactionHand) {
			if (!interactionEvent.isCanceled) {
				sendInteraction(targetedNode, interactionHand)
			}
		}

		event.isCanceled = true
	}

	private fun sendInteraction(targetedNode: TargetedWebNode, hand: InteractionHand) {
		val targetsNode = targetedNode.lineUuid == null
		val targetUuid = targetedNode.lineUuid ?: targetedNode.node.uuid

		WebLineInteractPacket(
			targetUuid,
			targetsNode,
			targetedNode.node.position,
			hand
		).messageServer()
	}

	private fun getInteractionHand(player: Player): InteractionHand? {
		if (player.mainHandItem.isItem(ModItemTagsProvider.WEB_LINE_INTERACTABLE)) {
			return InteractionHand.MAIN_HAND
		}

		if (player.offhandItem.isItem(ModItemTagsProvider.WEB_LINE_INTERACTABLE)) {
			return InteractionHand.OFF_HAND
		}

		return null
	}

	private fun getTargetedNode(
		player: Player,
		eyePosition: Vec3,
		viewVector: Vec3,
		interactionHand: InteractionHand
	): TargetedWebNode? {
		val lookEnd = eyePosition.add(viewVector.scale(player.blockInteractionRange()))

		val lines = ClientWebLines.getLines()
		val installedWebPort = WebLineInteractionHandler.getTargetedNode(
			lines,
			eyePosition,
			lookEnd,
			snapToExistingNode = true,
			requireExistingNode = true
		)

		if (installedWebPort != null && targetsInstalledWebPort(installedWebPort)) {
			if (isBehindCurrentHit(installedWebPort, eyePosition)) return null

			return installedWebPort
		}

		val heldStack = player.getItemInHand(interactionHand)
		return WebLineInteractionHandler.getTargetedNode(
			lines,
			eyePosition,
			lookEnd,
			shouldSnapToExistingNode(heldStack),
			requiresExistingNode(heldStack)
		)
	}

	private fun targetsInstalledWebPort(targetedNode: TargetedWebNode): Boolean {
		if (targetedNode.lineUuid != null) return false

		val blockAnchor = targetedNode.node as? WebBlockAnchor ?: return false
		return blockAnchor.hasWebPort
	}

	private fun isBehindCurrentHit(targetedNode: TargetedWebNode, eyePosition: Vec3): Boolean {
		val hitResult = Minecraft.getInstance().hitResult ?: return false
		if (hitResult.type == HitResult.Type.MISS) return false

		val hitDistanceSquared = eyePosition.distanceToSqr(hitResult.location)
		val webPortDistanceSquared = eyePosition.distanceToSqr(targetedNode.node.position)
		return hitDistanceSquared < webPortDistanceSquared
	}

	private fun shouldSnapToExistingNode(itemStack: ItemStack): Boolean {
		return itemStack.isItem(ModItems.ARTIFICIAL_SPINNERETS)
			|| requiresExistingNode(itemStack)
	}

	private fun requiresExistingNode(itemStack: ItemStack): Boolean {
		return itemStack.isItem(ModItems.WEB_PATHFINDER)
			|| itemStack.isItem(ModItems.WEB_PORT)
			|| !itemStack.isItem(ModItemTagsProvider.WEB_LINE_INTERACTABLE)
	}

}