package dev.aaronhowser.mods.critterworks.handler.web

import dev.aaronhowser.mods.aaron.misc.AaronExtensions.getEquipmentSlot
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isItem
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.status
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.toComponent
import dev.aaronhowser.mods.critterworks.datagen.language.ModMessageLang
import dev.aaronhowser.mods.critterworks.datagen.tag.ModItemTagsProvider
import dev.aaronhowser.mods.critterworks.event.custom.WebLineInteractionEvent
import dev.aaronhowser.mods.critterworks.event.custom.WebNodeInteractionEvent
import dev.aaronhowser.mods.critterworks.handler.web.line.WebLine
import dev.aaronhowser.mods.critterworks.handler.web.node.WebBlockAnchor
import dev.aaronhowser.mods.critterworks.handler.web.node.WebLineAnchor
import dev.aaronhowser.mods.critterworks.handler.web.node.WebNode
import dev.aaronhowser.mods.critterworks.item.component.WebNodeDataComponent
import dev.aaronhowser.mods.critterworks.menu.web_port.WebPortMenu
import dev.aaronhowser.mods.critterworks.packet.server_to_client.ShowWebPathPacket
import dev.aaronhowser.mods.critterworks.registry.ModDataComponents
import dev.aaronhowser.mods.critterworks.registry.ModItems
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
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
import net.minecraft.world.phys.Vec3
import net.neoforged.neoforge.common.Tags
import net.neoforged.neoforge.common.NeoForge
import org.joml.Intersectiond
import org.joml.Vector3d
import java.util.*

object WebLineInteractionHandler {

	private const val LINE_SELECTION_RADIUS = 0.3
	private const val REQUESTED_POSITION_TOLERANCE = 0.1

	private fun isWithinPositionTolerance(expected: Vec3, actual: Vec3): Boolean {
		val toleranceSquared = REQUESTED_POSITION_TOLERANCE * REQUESTED_POSITION_TOLERANCE
		return expected.distanceToSqr(actual) <= toleranceSquared
	}

	fun interact(
		player: ServerPlayer,
		targetUuid: UUID,
		targetsNode: Boolean,
		requestedPosition: Vec3,
		hand: InteractionHand
	) {
		val itemStack = player.getItemInHand(hand)

		if (targetsNode) {
			interactNode(player, targetUuid, requestedPosition, itemStack, hand)
			return
		}

		interactLine(player, targetUuid, requestedPosition, itemStack, hand)
	}

	private fun interactNode(
		player: ServerPlayer,
		targetUuid: UUID,
		requestedPosition: Vec3,
		itemStack: ItemStack,
		hand: InteractionHand
	) {
		val target = WebSavedData.get(player.serverLevel()).getNode(targetUuid) ?: return
		if (!isTargetingNode(player, target)) return

		if (!isWithinPositionTolerance(target.position, requestedPosition)) return

		NeoForge.EVENT_BUS.post(WebNodeInteractionEvent(player, target, itemStack, hand))
	}

	private fun interactLine(
		player: ServerPlayer,
		lineUuid: UUID,
		requestedPosition: Vec3,
		itemStack: ItemStack,
		hand: InteractionHand
	) {
		val line = WebSavedData.get(player.serverLevel()).getLine(lineUuid) ?: return

		val eyePosition = player.eyePosition
		val lookEnd = eyePosition.add(player.lookAngle.scale(player.blockInteractionRange()))
		val snapToExistingNode = itemStack.isItem(ModItemTagsProvider.SNAP_TO_NODE)
		val requireExistingNode = itemStack.isItem(ModItemTagsProvider.REQUIRES_EXISTING_NODE)
		val targetedNode = getTargetedNode(
			listOf(line),
			eyePosition,
			lookEnd,
			snapToExistingNode,
			requireExistingNode
		) ?: return

		if (targetedNode.node !is WebLineAnchor) return
		if (!isWithinPositionTolerance(targetedNode.node.position, requestedPosition)) return

		NeoForge.EVENT_BUS.post(
			WebLineInteractionEvent(player, lineUuid, targetedNode.node, itemStack, hand)
		)
	}

	fun handleNodeInteraction(event: WebNodeInteractionEvent) {
		val player = event.player as? ServerPlayer ?: return
		val level = player.serverLevel()

		val savedData = WebSavedData.get(level)
		val selectedNode = savedData.getNode(event.node.uuid) ?: return

		if (!isTargetingNode(player, selectedNode)) return
		if (!isWithinPositionTolerance(selectedNode.position, event.node.position)) return

		if (selectedNode is WebBlockAnchor && selectedNode.hasWebPort) {
			if (event.itemStack.isEmpty && player.isSecondaryUseActive) {
				val removedStack = savedData.removeWebPort(level, selectedNode)
				player.drop(removedStack, false)
			} else {
				openWebPort(player, selectedNode)
			}

			return
		}

		if (!event.itemStack.isItem(ModItemTagsProvider.WEB_LINE_INTERACTABLE)) return

		when {
			event.itemStack.isItem(ModItems.WEB_PORT) -> {
				val blockAnchor = selectedNode as? WebBlockAnchor ?: return
				if (blockAnchor.hasWebPort) return

				savedData.installWebPort(level, blockAnchor, event.itemStack)
				event.itemStack.consume(1, player)
			}

			event.itemStack.isItem(ModItems.WEB_PATHFINDER) ->
				handlePathSelection(level, player, event.itemStack, selectedNode)

			else ->
				handleNodeSelection(level, player, event.itemStack, selectedNode)
		}
	}

	fun handleLineInteraction(player: ServerPlayer, event: WebLineInteractionEvent) {
		val player = event.player as? ServerPlayer ?: return
		val level = player.serverLevel()
		val savedData = WebSavedData.get(level)

		if (!event.itemStack.isItem(ModItemTagsProvider.WEB_LINE_INTERACTABLE)) return

		val line = savedData.getLine(event.lineUuid) ?: return

		val eyePosition = player.eyePosition
		val interactionRange = player.blockInteractionRange()
		val lookOffset = player.lookAngle.scale(interactionRange)
		val lookEnd = eyePosition.add(lookOffset)

		val snapToExistingNode = event.itemStack.isItem(ModItemTagsProvider.SNAP_TO_NODE)
		val requireExistingNode = event.itemStack.isItem(ModItemTagsProvider.REQUIRES_EXISTING_NODE)

		val targetedNode = getTargetedNode(
			listOf(line),
			eyePosition,
			lookEnd,
			snapToExistingNode,
			requireExistingNode
		) ?: return

		val selectedNode = targetedNode.node

		if (!isWithinPositionTolerance(selectedNode.position, event.target.position)) return

		when {
			event.itemStack.isItem(Tags.Items.TOOLS_SHEAR) && selectedNode is WebLineAnchor ->
				shearLine(level, player, event.itemStack, event.lineUuid, selectedNode, event.hand)

			event.itemStack.isItem(ModItems.ARTIFICIAL_SPINNERETS) ->
				handleNodeSelection(level, player, event.itemStack, selectedNode)

			event.itemStack.isItem(ModItems.WEB_PATHFINDER) ->
				handlePathSelection(level, player, event.itemStack, selectedNode)
		}
	}

	private fun openWebPort(player: ServerPlayer, anchor: WebBlockAnchor) {
		val constructor = MenuConstructor { containerId, inventory, _ ->
			WebPortMenu(containerId, inventory, anchor)
		}

		val provider = SimpleMenuProvider(constructor, anchor.webPort.hoverName)
		player.openMenu(provider) { data ->
			data.writeBoolean(true)
			data.writeUUID(anchor.uuid)
			ItemStack.OPTIONAL_STREAM_CODEC.encode(data, anchor.webPort)
		}
	}

	private fun handlePathSelection(
		level: ServerLevel,
		player: ServerPlayer,
		itemStack: ItemStack,
		selectedNode: WebNode
	) {
		val firstNode = itemStack.get(ModDataComponents.WEB_NODE)?.node
		if (firstNode == null) {
			storeFirstNode(player, itemStack, selectedNode)
			return
		}

		val savedData = WebSavedData.get(level)
		val canonicalFirstNode = savedData.getCanonicalNode(firstNode)
		val canonicalSelectedNode = savedData.getCanonicalNode(selectedNode)
		val network = canonicalFirstNode.lines.firstOrNull()?.network
		val path = network?.findShortestPath(canonicalFirstNode, canonicalSelectedNode)

		itemStack.remove(ModDataComponents.WEB_NODE)
		if (path == null) return

		val positions: MutableList<Vec3> = mutableListOf(path.startNode.position)
		for (segment in path.segments) {
			positions.add(segment.toNode.position)
		}

		ShowWebPathPacket(positions).messagePlayer(player)
	}

	fun handleNodeSelection(
		level: ServerLevel,
		player: Player,
		itemStack: ItemStack,
		selectedNode: WebNode
	) {
		val firstNode = itemStack.get(ModDataComponents.WEB_NODE)?.node

		if (firstNode == null) {
			storeFirstNode(player, itemStack, selectedNode)
		} else {
			createLine(level, player, itemStack, firstNode, selectedNode)
		}
	}

	private fun createLine(
		level: ServerLevel,
		player: Player,
		itemStack: ItemStack,
		firstNode: WebNode,
		selectedNode: WebNode
	) {
		val savedData = WebSavedData.get(level)
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

		val webLine = WebLine(UUID.randomUUID(), canonicalFirstNode, canonicalSelectedNode)
		savedData.addLine(level, webLine)

		itemStack.remove(ModDataComponents.WEB_NODE)
		player.status(ModMessageLang.LINE_CREATED_MESSAGE.toComponent())
	}

	private fun shearLine(
		level: ServerLevel,
		player: ServerPlayer,
		itemStack: ItemStack,
		lineUuid: UUID,
		selectedNode: WebLineAnchor,
		hand: InteractionHand
	) {
		WebSavedData.get(level).removeLine(level, lineUuid)
		itemStack.hurtAndBreak(1, player, hand.getEquipmentSlot())

		player.status(ModMessageLang.LINE_REMOVED_MESSAGE.toComponent())

		level.gameEvent(player, GameEvent.SHEAR, selectedNode.position)
	}

	private fun storeFirstNode(player: Player, itemStack: ItemStack, selectedNode: WebNode) {
		itemStack.set(ModDataComponents.WEB_NODE, WebNodeDataComponent(selectedNode))
		player.status(ModMessageLang.FIRST_NODE_MESSAGE.toComponent())
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

	fun createBlockAnchor(blockPos: BlockPos, face: Direction, position: Vec3): WebBlockAnchor {
		val surfaceOffset = 0.001
		val faceNormal = Vec3.atLowerCornerOf(face.normal)

		return WebBlockAnchor(
			UUID.randomUUID(),
			blockPos,
			face,
			position.add(faceNormal.scale(surfaceOffset))
		)
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
			&& firstNode.lineUuid == secondNode.lineUuid
		) return ModMessageLang.SAME_LINE_MESSAGE

		if (firstNode is WebBlockAnchor
			&& secondNode is WebBlockAnchor
			&& firstNode.face == secondNode.face
		) return ModMessageLang.SAME_DIRECTION_MESSAGE

		val maxLength = 10.0
		if (firstNode.position.distanceToSqr(secondNode.position) >= maxLength * maxLength) {
			return ModMessageLang.TOO_LONG_MESSAGE
		}

		if (!player.hasInfiniteMaterials()) {
			val lineLength = firstNode.position.distanceTo(secondNode.position)
			val availableWebFluid = itemStack.getOrDefault(ModDataComponents.WEB_FLUID, 0.0)
			if (lineLength > availableWebFluid) return ModMessageLang.NOT_ENOUGH_WEB_FLUID_MESSAGE
		}

		if (!hasLineOfSight(level, player, firstNode, secondNode)) {
			return ModMessageLang.OBSTRUCTED_MESSAGE
		}

		return null
	}

	fun getTargetedNode(
		lines: List<WebLine>,
		lookStart: Vec3,
		lookEnd: Vec3,
		snapToExistingNode: Boolean,
		requireExistingNode: Boolean = false
	): TargetedWebNode? {
		val selectionRadiusSquared = LINE_SELECTION_RADIUS * LINE_SELECTION_RADIUS
		var targetedNode: TargetedWebNode? = null
		var targetedDistanceSquared = selectionRadiusSquared

		for (line in lines) {
			val lineStart = line.firstNode.position
			val lineEnd = line.secondNode.position
			val anchorPosition = Vector3d()
			val lookPosition = Vector3d()

			val distanceFromLookSquared = Intersectiond.findClosestPointsLineSegments(
				lineStart.x, lineStart.y, lineStart.z,
				lineEnd.x, lineEnd.y, lineEnd.z,
				lookStart.x, lookStart.y, lookStart.z,
				lookEnd.x, lookEnd.y, lookEnd.z,
				anchorPosition,
				lookPosition
			)

			if (distanceFromLookSquared > targetedDistanceSquared) continue

			val position = Vec3(anchorPosition.x, anchorPosition.y, anchorPosition.z)
			val existingNode = getClosestExistingNode(line, position)
			if (requireExistingNode && existingNode == null) continue

			targetedDistanceSquared = distanceFromLookSquared

			val node = if (snapToExistingNode && existingNode != null) {
				existingNode
			} else {
				WebLineAnchor(UUID.randomUUID(), line.uuid, position)
			}

			val targetedLineUuid = if (node === line.firstNode || node === line.secondNode) {
				null
			} else {
				line.uuid
			}

			targetedNode = TargetedWebNode(targetedLineUuid, node)
		}

		return targetedNode
	}

	private fun getClosestExistingNode(line: WebLine, position: Vec3): WebNode? {
		val snapRadiusSquared = NODE_SNAP_RADIUS * NODE_SNAP_RADIUS
		var closestNode = getCloserNode(null, line.firstNode, position, snapRadiusSquared)
		closestNode = getCloserNode(closestNode, line.secondNode, position, snapRadiusSquared)

		for (attachment in line.attachedAnchors) {
			closestNode = getCloserNode(
				closestNode,
				attachment.anchor,
				position,
				snapRadiusSquared
			)
		}

		return closestNode
	}

	private fun getCloserNode(
		closestNode: WebNode?,
		candidateNode: WebNode,
		position: Vec3,
		maximumDistanceSquared: Double
	): WebNode? {
		val candidateDistanceSquared = candidateNode.position.distanceToSqr(position)
		if (candidateDistanceSquared > maximumDistanceSquared) return closestNode

		if (closestNode != null) {
			val closestDistanceSquared = closestNode.position.distanceToSqr(position)
			if (closestDistanceSquared < candidateDistanceSquared) return closestNode
		}

		return candidateNode
	}

	private fun hasLineOfSight(
		level: Level,
		player: Player,
		firstNode: WebNode,
		secondNode: WebNode
	): Boolean {
		val clipContext = ClipContext(
			firstNode.position,
			secondNode.position,
			ClipContext.Block.COLLIDER,
			ClipContext.Fluid.NONE,
			player
		)
		val result = level.clip(clipContext)
		return result.type == HitResult.Type.MISS
	}

	private fun isTargetingNode(player: Player, node: WebNode): Boolean {
		val lookStart = player.eyePosition
		val lookOffset = player.lookAngle.scale(player.blockInteractionRange())
		val lookLengthSquared = lookOffset.lengthSqr()
		if (lookLengthSquared == 0.0) return false

		val nodeOffset = node.position.subtract(lookStart)
		val progress = nodeOffset.dot(lookOffset)
			.div(lookLengthSquared)
			.coerceIn(0.0, 1.0)
		val closestPosition = lookStart.add(lookOffset.scale(progress))
		val selectionRadiusSquared = LINE_SELECTION_RADIUS * LINE_SELECTION_RADIUS

		return closestPosition.distanceToSqr(node.position) <= selectionRadiusSquared
	}

	private const val NODE_SNAP_RADIUS = 0.3

}