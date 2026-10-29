package dev.aaronhowser.mods.critterworks.handler.web.common.line

import dev.aaronhowser.mods.critterworks.handler.web.common.node.TargetedWebNode
import dev.aaronhowser.mods.critterworks.handler.web.common.node.WebLineAnchor
import dev.aaronhowser.mods.critterworks.handler.web.common.node.WebNode
import net.minecraft.world.phys.Vec3
import org.joml.Intersectiond
import org.joml.Vector3d
import java.util.*

object WebTargeting {

	fun getTargetedNode(
		lines: Collection<WebLine>,
		lookStart: Vec3,
		lookEnd: Vec3,
		snapToExistingNode: Boolean,
		requireExistingNode: Boolean = false
	): TargetedWebNode? {
		val selectionRadiusSquared = LINE_SELECTION_RADIUS * LINE_SELECTION_RADIUS
		var targetedNode: TargetedWebNode? = null
		var targetedDistanceSquared = selectionRadiusSquared

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
		return closestPosition.distanceToSqr(node.position) <= LINE_SELECTION_RADIUS * LINE_SELECTION_RADIUS
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

	private const val LINE_SELECTION_RADIUS = 0.3
	private const val NODE_SNAP_RADIUS = 0.3
}