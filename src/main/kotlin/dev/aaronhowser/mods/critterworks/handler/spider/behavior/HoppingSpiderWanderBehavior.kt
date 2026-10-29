package dev.aaronhowser.mods.critterworks.handler.spider.behavior

import dev.aaronhowser.mods.aaron.misc.AaronExtensions.chance
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.nextRange
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.oneIn
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.random
import dev.aaronhowser.mods.critterworks.handler.spider.HoppingSpider
import dev.aaronhowser.mods.critterworks.handler.web.common.node.WebNode
import dev.aaronhowser.mods.critterworks.handler.web.item.ItemWebHandler
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.phys.Vec3
import java.util.*

class HoppingSpiderWanderBehavior(
	private val homeNodeUuid: UUID,
	override var currentNodeUuid: UUID = homeNodeUuid,
	private var previousNodeUuid: UUID? = null,
	private var targetNodeUuid: UUID? = null,
	private var pauseUntil: Long = 0
) : HoppingSpiderBehavior {

	override val priority: Int = 0
	override val canBeInterrupted: Boolean
		get() = targetNodeUuid == null

	override fun tick(level: ServerLevel, spider: HoppingSpider, nestPosition: Vec3): Boolean {
		val savedData = ItemWebHandler.get(level)
		val currentNode = savedData.getNode(currentNodeUuid)
		if (currentNode == null) {
			spider.stopBehavior(this)
			return true
		}

		if (targetNodeUuid == null) {
			spider.position = currentNode.position
			if (level.gameTime < pauseUntil) return false
			if (level.isNight && currentNodeUuid == homeNodeUuid) {
				pauseUntil = level.gameTime + level.random.nextRange(
					MIN_NIGHT_PAUSE_TICKS,
					MAX_NIGHT_PAUSE_TICKS + 1
				)
				return true
			}

			targetNodeUuid = chooseTarget(level, currentNode)?.uuid ?: return false
			spider.clearRoute()
		}

		val targetNodeUuid = targetNodeUuid ?: return false
		return when (spider.travelTo(level, currentNodeUuid, targetNodeUuid)) {
			HoppingSpider.TravelResult.STARTED -> true
			HoppingSpider.TravelResult.TRAVELLING -> false
			HoppingSpider.TravelResult.MISSING_PATH -> {
				this.targetNodeUuid = null
				spider.clearRoute()
				true
			}

			HoppingSpider.TravelResult.ARRIVED -> {
				previousNodeUuid = currentNodeUuid
				currentNodeUuid = targetNodeUuid
				this.targetNodeUuid = null
				pauseUntil = getNextPause(level)
				spider.clearRoute()
				true
			}
		}
	}

	private fun chooseTarget(level: ServerLevel, currentNode: WebNode): WebNode? {
		if (level.isNight && currentNode.uuid != homeNodeUuid) {
			return ItemWebHandler.get(level).getNode(homeNodeUuid)
		}

		val network = ItemWebHandler.get(level).getNetwork(currentNode) ?: return null
		val random = level.random

		if (random.oneIn(DISTANT_TARGET_CHANCE)) {
			val distantNodes = network.getNodes().filter { node -> node.uuid != currentNode.uuid }
			if (distantNodes.isNotEmpty()) return distantNodes.random(random)
		}

		val adjacentNodes = mutableListOf<WebNode>()
		for (line in currentNode.lines) {
			val adjacentNode = if (line.firstNode.uuid == currentNode.uuid) line.secondNode else line.firstNode
			if (adjacentNode.uuid != previousNodeUuid) adjacentNodes.add(adjacentNode)
		}

		if (adjacentNodes.isEmpty()) {
			for (line in currentNode.lines) {
				adjacentNodes.add(if (line.firstNode.uuid == currentNode.uuid) line.secondNode else line.firstNode)
			}
		}

		if (adjacentNodes.isEmpty()) return null
		return adjacentNodes.random(random)
	}

	private fun getNextPause(level: ServerLevel): Long {
		if (!level.random.chance(PAUSE_CHANCE)) return level.gameTime
		return level.gameTime + level.random.nextRange(MIN_PAUSE_TICKS, MAX_PAUSE_TICKS + 1)
	}

	override fun save(): CompoundTag {
		val tag = CompoundTag()
		tag.putString(TYPE_TAG, TYPE)
		tag.putUUID(HOME_NODE_TAG, homeNodeUuid)
		tag.putUUID(CURRENT_NODE_TAG, currentNodeUuid)
		tag.putLong(PAUSE_UNTIL_TAG, pauseUntil)

		val previousNodeUuid = previousNodeUuid
		if (previousNodeUuid != null) tag.putUUID(PREVIOUS_NODE_TAG, previousNodeUuid)

		val targetNodeUuid = targetNodeUuid
		if (targetNodeUuid != null) tag.putUUID(TARGET_NODE_TAG, targetNodeUuid)

		return tag
	}

	companion object {
		const val TYPE = "wander"
		private const val TYPE_TAG = "Type"
		private const val HOME_NODE_TAG = "HomeNode"
		private const val CURRENT_NODE_TAG = "CurrentNode"
		private const val PREVIOUS_NODE_TAG = "PreviousNode"
		private const val TARGET_NODE_TAG = "TargetNode"
		private const val PAUSE_UNTIL_TAG = "PauseUntil"
		private const val DISTANT_TARGET_CHANCE = 8
		private const val PAUSE_CHANCE = 0.7f
		private const val MIN_PAUSE_TICKS = 5
		private const val MAX_PAUSE_TICKS = 30
		private const val MIN_NIGHT_PAUSE_TICKS = 40
		private const val MAX_NIGHT_PAUSE_TICKS = 100

		fun load(tag: CompoundTag): HoppingSpiderWanderBehavior? {
			if (!tag.hasUUID(CURRENT_NODE_TAG)) return null

			val currentNodeUuid = tag.getUUID(CURRENT_NODE_TAG)
			val homeNodeUuid = if (tag.hasUUID(HOME_NODE_TAG)) tag.getUUID(HOME_NODE_TAG) else currentNodeUuid

			return HoppingSpiderWanderBehavior(
				homeNodeUuid,
				currentNodeUuid,
				if (tag.hasUUID(PREVIOUS_NODE_TAG)) tag.getUUID(PREVIOUS_NODE_TAG) else null,
				if (tag.hasUUID(TARGET_NODE_TAG)) tag.getUUID(TARGET_NODE_TAG) else null,
				tag.getLong(PAUSE_UNTIL_TAG)
			)
		}
	}
}