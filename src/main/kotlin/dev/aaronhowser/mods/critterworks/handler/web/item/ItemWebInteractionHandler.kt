package dev.aaronhowser.mods.critterworks.handler.web.item

import dev.aaronhowser.mods.aaron.misc.AaronExtensions.getEquipmentSlot
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isItem
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.status
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.toComponent
import dev.aaronhowser.mods.critterworks.datagen.language.ModMessageLang
import dev.aaronhowser.mods.critterworks.datagen.tag.ModItemTagsProvider
import dev.aaronhowser.mods.critterworks.event.custom.WebLineInteractionEvent
import dev.aaronhowser.mods.critterworks.event.custom.WebNodeInteractionEvent
import dev.aaronhowser.mods.critterworks.handler.web.WebLineType
import dev.aaronhowser.mods.critterworks.handler.web.common.line.WebLine
import dev.aaronhowser.mods.critterworks.handler.web.common.node.WebBlockAnchor
import dev.aaronhowser.mods.critterworks.handler.web.common.node.WebLineAnchor
import dev.aaronhowser.mods.critterworks.handler.web.common.node.WebNode
import dev.aaronhowser.mods.critterworks.item.component.WebNodeDataComponent
import dev.aaronhowser.mods.critterworks.menu.web_port.WebPortMenu
import dev.aaronhowser.mods.critterworks.registry.ModDataComponents
import dev.aaronhowser.mods.critterworks.registry.ModItems
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.SimpleMenuProvider
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.MenuConstructor
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.gameevent.GameEvent
import net.minecraft.world.phys.HitResult
import net.neoforged.neoforge.common.Tags
import java.util.*

object ItemWebInteractionHandler {

	fun handleNodeInteraction(event: WebNodeInteractionEvent) {
		if (event.webType != WebLineType.ITEM) return

		val player = event.player as? ServerPlayer ?: return
		val level = player.serverLevel()
		val savedData = ItemWebHandler.get(level)
		val selectedNode = savedData.getNode(event.node.uuid) ?: return
		val stack = event.itemStack

		if (selectedNode is WebBlockAnchor && savedData.hasWebPort(selectedNode)) {
			if (stack.isEmpty && player.isSecondaryUseActive) {
				player.drop(savedData.removeWebPort(level, selectedNode), false)
			} else {
				openWebPort(player, selectedNode)
			}
			return
		}

		if (!stack.isItem(ModItemTagsProvider.ITEM_WEB_LINE_INTERACTABLE)) return
		if (stack.isItem(ModItems.WEB_PORT)) {
			val blockAnchor = selectedNode as? WebBlockAnchor ?: return
			savedData.installWebPort(level, blockAnchor, stack)
			stack.consume(1, player)
			return
		}

		handleNodeSelection(level, player, stack, selectedNode)
	}

	fun handleLineInteraction(event: WebLineInteractionEvent) {
		if (event.webType != WebLineType.ITEM) return

		val player = event.player as? ServerPlayer ?: return
		val stack = event.itemStack
		if (!stack.isItem(ModItemTagsProvider.ITEM_WEB_LINE_INTERACTABLE)) return
		if (ItemWebHandler.get(player.serverLevel()).getLine(event.lineUuid) == null) return

		when {
			stack.isItem(Tags.Items.TOOLS_SHEAR) -> shearLine(player, stack, event)
			stack.isItem(ModItems.ARTIFICIAL_SPINNERETS) ->
				handleNodeSelection(player.serverLevel(), player, stack, event.target)
		}
	}

	fun handleNodeSelection(
		level: ServerLevel,
		player: Player,
		itemStack: ItemStack,
		selectedNode: WebNode
	) {
		val firstNode = itemStack.get(ModDataComponents.WEB_NODE)?.node
		if (firstNode == null) {
			itemStack.set(ModDataComponents.WEB_NODE, WebNodeDataComponent(selectedNode))
			player.status(ModMessageLang.FIRST_NODE_MESSAGE.toComponent())
			return
		}

		createLine(level, player, itemStack, firstNode, selectedNode)
	}

	fun canCreateLine(
		level: Level,
		player: Player,
		itemStack: ItemStack,
		firstNode: WebNode,
		secondNode: WebNode
	): Boolean {
		return getInvalidMessage(level, player, itemStack, firstNode, secondNode) == null
	}

	private fun createLine(
		level: ServerLevel,
		player: Player,
		itemStack: ItemStack,
		firstNode: WebNode,
		selectedNode: WebNode
	) {
		val savedData = ItemWebHandler.get(level)
		if (firstNode is WebLineAnchor && savedData.getLine(firstNode.parentLineUuid) == null) {
			itemStack.remove(ModDataComponents.WEB_NODE)
			return
		}

		val canonicalFirstNode = savedData.getCanonicalNode(firstNode)
		val canonicalSelectedNode = savedData.getCanonicalNode(selectedNode)
		val invalidMessage = getInvalidMessage(
			level,
			player,
			itemStack,
			canonicalFirstNode,
			canonicalSelectedNode
		)
		if (invalidMessage != null) {
			player.status(invalidMessage.toComponent())
			return
		}

		if (!player.hasInfiniteMaterials()) {
			val lineLength = canonicalFirstNode.position.distanceTo(canonicalSelectedNode.position)
			val remainingWebFluid = itemStack.getOrDefault(ModDataComponents.WEB_FLUID, 0.0) - lineLength
			itemStack.set(ModDataComponents.WEB_FLUID, remainingWebFluid.coerceAtLeast(0.0))
		}

		savedData.addLine(
			level,
			WebLine(UUID.randomUUID(), canonicalFirstNode, canonicalSelectedNode)
		)
		itemStack.remove(ModDataComponents.WEB_NODE)
		player.status(ModMessageLang.LINE_CREATED_MESSAGE.toComponent())
	}

	private fun getInvalidMessage(
		level: Level,
		player: Player,
		itemStack: ItemStack,
		firstNode: WebNode,
		secondNode: WebNode
	): String? {
		if (firstNode.uuid == secondNode.uuid) return ModMessageLang.SAME_LINE_MESSAGE
		if (firstNode is WebLineAnchor
			&& secondNode is WebLineAnchor
			&& firstNode.parentLineUuid == secondNode.parentLineUuid
		) return ModMessageLang.SAME_LINE_MESSAGE
		if (firstNode is WebBlockAnchor
			&& secondNode is WebBlockAnchor
			&& firstNode.face == secondNode.face
		) return ModMessageLang.SAME_DIRECTION_MESSAGE
		if (firstNode.position.distanceToSqr(secondNode.position) >= MAX_LENGTH * MAX_LENGTH) {
			return ModMessageLang.TOO_LONG_MESSAGE
		}

		if (!player.hasInfiniteMaterials()) {
			val lineLength = firstNode.position.distanceTo(secondNode.position)
			val availableWebFluid = itemStack.getOrDefault(ModDataComponents.WEB_FLUID, 0.0)
			if (lineLength > availableWebFluid) return ModMessageLang.NOT_ENOUGH_WEB_FLUID_MESSAGE
		}

		val clipContext = ClipContext(
			firstNode.position,
			secondNode.position,
			ClipContext.Block.COLLIDER,
			ClipContext.Fluid.NONE,
			player
		)
		if (level.clip(clipContext).type != HitResult.Type.MISS) return ModMessageLang.OBSTRUCTED_MESSAGE

		return null
	}

	private fun shearLine(
		player: ServerPlayer,
		itemStack: ItemStack,
		event: WebLineInteractionEvent
	) {
		val level = player.serverLevel()
		if (ItemWebHandler.get(level).removeLine(level, event.lineUuid) == null) return

		itemStack.hurtAndBreak(1, player, event.hand.getEquipmentSlot())
		player.status(ModMessageLang.LINE_REMOVED_MESSAGE.toComponent())
		level.gameEvent(player, GameEvent.SHEAR, event.target.position)
	}

	private fun openWebPort(player: ServerPlayer, anchor: WebBlockAnchor) {
		val webPort = ItemWebHandler.get(player.serverLevel()).getWebPort(anchor)
		val constructor = MenuConstructor { containerId, inventory, _ ->
			WebPortMenu(containerId, inventory, anchor)
		}

		player.openMenu(SimpleMenuProvider(constructor, webPort.hoverName)) { data ->
			data.writeBoolean(true)
			data.writeUUID(anchor.uuid)
			ItemStack.OPTIONAL_STREAM_CODEC.encode(data, webPort)
		}
	}

	private const val MAX_LENGTH = 10.0
}