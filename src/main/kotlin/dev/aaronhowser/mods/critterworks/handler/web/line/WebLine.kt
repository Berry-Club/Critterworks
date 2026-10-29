package dev.aaronhowser.mods.critterworks.handler.web.line

import dev.aaronhowser.mods.critterworks.handler.web.WebNetwork
import dev.aaronhowser.mods.critterworks.handler.web.graph.WebEdge
import dev.aaronhowser.mods.critterworks.handler.web.node.WebLineAnchor
import dev.aaronhowser.mods.critterworks.handler.web.node.WebNode
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.shapes.CollisionContext
import java.util.*

data class WebLine(
	override val uuid: UUID,
	val firstNode: WebNode,
	val secondNode: WebNode
) : WebEdge<WebNode>() {
	override val firstVertex: WebNode
		get() = firstNode

	override val secondVertex: WebNode
		get() = secondNode

	var network: WebNetwork? = null

	private val attachmentsByAnchorUuid: MutableMap<UUID, LineAnchorAttachment> = mutableMapOf()

	val attachedAnchors: Collection<LineAnchorAttachment>
		get() = attachmentsByAnchorUuid.values

	val data = WebLineData(uuid, firstNode.uuid, secondNode.uuid)

	fun addAttachedAnchor(anchor: WebLineAnchor) {
		attachmentsByAnchorUuid[anchor.uuid] = LineAnchorAttachment(
			anchor,
			anchor.position.distanceTo(firstNode.position),
			anchor.position.distanceTo(secondNode.position)
		)
		network?.invalidatePaths()
	}

	fun removeAttachedAnchor(anchorUuid: UUID) {
		if (attachmentsByAnchorUuid.remove(anchorUuid) != null) {
			network?.invalidatePaths()
		}
	}

	fun clearAttachedAnchors() {
		if (attachmentsByAnchorUuid.isEmpty()) return

		attachmentsByAnchorUuid.clear()
		network?.invalidatePaths()
	}

	fun getInvalidation(
		level: ServerLevel,
		lines: Map<UUID, WebLine>
	): WebLineInvalidation? {
		return getInvalidation(level, lines, mutableSetOf())
	}

	fun getInvalidation(
		level: ServerLevel,
		lines: Map<UUID, WebLine>,
		checkedLines: MutableSet<UUID>
	): WebLineInvalidation? {
		if (!checkedLines.add(uuid)) {
			return WebLineInvalidation(WebLineInvalidationReason.CYCLIC_DEPENDENCY)
		}

		val firstInvalidation = firstNode.getInvalidation(level, lines, checkedLines)
		if (firstInvalidation != null) return firstInvalidation

		val secondInvalidation = secondNode.getInvalidation(level, lines, checkedLines)
		if (secondInvalidation != null) return secondInvalidation

		checkedLines.remove(uuid)

		val clipContext = ClipContext(
			firstNode.position,
			secondNode.position,
			ClipContext.Block.COLLIDER,
			ClipContext.Fluid.NONE,
			CollisionContext.empty()
		)

		val hitResult = level.clip(clipContext)
		if (hitResult.type == HitResult.Type.MISS) return null

		return WebLineInvalidation(WebLineInvalidationReason.OBSTRUCTED)
	}

}