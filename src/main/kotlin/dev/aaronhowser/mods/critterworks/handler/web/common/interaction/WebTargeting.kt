package dev.aaronhowser.mods.critterworks.handler.web.common.interaction

import dev.aaronhowser.mods.critterworks.handler.web.common.line.WebLine
import dev.aaronhowser.mods.critterworks.handler.web.common.node.WebLineAnchor
import dev.aaronhowser.mods.critterworks.handler.web.common.node.WebNode
import net.minecraft.world.phys.Vec3
import org.joml.Intersectiond
import org.joml.Vector3d
import java.util.*

object WebTargeting {

	/** Finds the closest line being looked at and optionally snaps to a nearby node. */
	fun getTargetedNode(
		lines: Collection<WebLine>,
		lookStart: Vec3,
		lookEnd: Vec3,
		snapToExistingNode: Boolean,
		requireExistingNode: Boolean = false
	): TargetedWebNode? {
		var targetedNode: TargetedWebNode? = null
		var targetedDistanceSquared = LINE_SELECTION_RADIUS_SQUARED

		for (line in lines) {
			val anchorPosition = Vector3d()
			val lookPosition = Vector3d()
			val distanceFromLookSquared = Intersectiond.findClosestPointsLineSegments(
				line.firstNode.position.x, line.firstNode.position.y, line.firstNode.position.z,
				line.secondNode.position.x, line.secondNode.position.y, line.secondNode.position.z,
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

			val lineUuid = if (node === line.firstNode || node === line.secondNode) null else line.uuid
			targetedNode = TargetedWebNode(lineUuid, node)
		}

		return targetedNode
	}

	fun isTargetingNode(lookStart: Vec3, lookEnd: Vec3, node: WebNode): Boolean {
		val lookOffset = lookEnd.subtract(lookStart)
		val lookLengthSquared = lookOffset.lengthSqr()
		if (lookLengthSquared == 0.0) return false

		val progress = node.position
			.subtract(lookStart)
			.dot(lookOffset)
			.div(lookLengthSquared)
			.coerceIn(0.0, 1.0)

		val closestPosition = lookStart.add(lookOffset.scale(progress))
		return closestPosition.distanceToSqr(node.position) <= LINE_SELECTION_RADIUS_SQUARED
	}

	/** Finds the closest existing node. Ties use the node closest to the player. */
	fun getTargetedExistingNode(
		nodes: Collection<WebNode>,
		lookStart: Vec3,
		lookEnd: Vec3
	): TargetedWebNode? {
		val lookOffset = lookEnd.subtract(lookStart)
		val lookLengthSquared = lookOffset.lengthSqr()
		if (lookLengthSquared == 0.0) return null

		var targetedNode: WebNode? = null
		var targetedDistanceSquared = LINE_SELECTION_RADIUS_SQUARED
		var targetedEyeDistanceSquared = Double.MAX_VALUE

		for (node in nodes) {
			val progress = node.position
				.subtract(lookStart)
				.dot(lookOffset)
				.div(lookLengthSquared)
				.coerceIn(0.0, 1.0)
			val closestPosition = lookStart.add(lookOffset.scale(progress))
			val distanceSquared = closestPosition.distanceToSqr(node.position)
			if (distanceSquared > targetedDistanceSquared) continue

			val eyeDistanceSquared = lookStart.distanceToSqr(node.position)
			val isFartherFromEye = distanceSquared == targetedDistanceSquared &&
				eyeDistanceSquared >= targetedEyeDistanceSquared
			if (isFartherFromEye) continue

			targetedNode = node
			targetedDistanceSquared = distanceSquared
			targetedEyeDistanceSquared = eyeDistanceSquared
		}

		val node = targetedNode ?: return null

		return TargetedWebNode(null, node)
	}

	private fun getClosestExistingNode(line: WebLine, position: Vec3): WebNode? {
		var closestNode = getCloserNode(null, line.firstNode, position)
		closestNode = getCloserNode(closestNode, line.secondNode, position)

		for (attachment in line.attachedAnchors) {
			closestNode = getCloserNode(
				closestNode,
				attachment.anchor,
				position
			)
		}

		return closestNode
	}

	private fun getCloserNode(
		closestNode: WebNode?,
		candidateNode: WebNode,
		position: Vec3
	): WebNode? {
		val candidateDistanceSquared = candidateNode.position.distanceToSqr(position)
		if (candidateDistanceSquared > NODE_SNAP_RADIUS_SQUARED) return closestNode

		if (closestNode != null) {
			val closestDistanceSquared = closestNode.position.distanceToSqr(position)

			if (closestDistanceSquared < candidateDistanceSquared) return closestNode
		}

		return candidateNode
	}

	private const val LINE_SELECTION_RADIUS_SQUARED = 0.3 * 0.3
	private const val NODE_SNAP_RADIUS_SQUARED = 0.3 * 0.3
}