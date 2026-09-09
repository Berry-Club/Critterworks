package dev.aaronhowser.mods.critterworks.handler.spider

import dev.aaronhowser.mods.critterworks.handler.spider.behavior.HoppingSpiderBehavior
import dev.aaronhowser.mods.critterworks.handler.spider.behavior.HoppingSpiderWanderBehavior
import dev.aaronhowser.mods.critterworks.handler.spider.behavior.transport.HoppingSpiderTransportBehavior
import dev.aaronhowser.mods.critterworks.handler.web.WebSavedData
import dev.aaronhowser.mods.critterworks.handler.web.node.WebBlockAnchor
import dev.aaronhowser.mods.critterworks.handler.web.path.WebPath
import dev.aaronhowser.mods.critterworks.item.ItemFilterItem
import dev.aaronhowser.mods.critterworks.item.WebPortItem
import dev.aaronhowser.mods.critterworks.item.component.WebPortComponent
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.Containers
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.items.IItemHandler
import java.util.*

class HoppingSpider(
	val uuid: UUID = UUID.randomUUID(),
	val customName: String? = null
) {

	var activeBehavior: HoppingSpiderBehavior? = null
	var transportBehavior: HoppingSpiderTransportBehavior?
		get() = activeBehavior as? HoppingSpiderTransportBehavior
		set(value) {
			activeBehavior = value
		}
	var carriedStack: ItemStack = ItemStack.EMPTY
	var position: Vec3? = null
	var route: HoppingSpiderRoute? = null
	var routeProgress: Double = 0.0

	fun serverTick(level: ServerLevel, nestPosition: Vec3): Boolean {
		if (position == null) {
			position = nestPosition
		}

		val activeBehavior = activeBehavior ?: return false
		return activeBehavior.tick(level, this, nestPosition)
	}

	fun tryStartBehavior(behavior: HoppingSpiderBehavior): Boolean {
		val activeBehavior = activeBehavior
		if (activeBehavior != null) {
			if (!activeBehavior.canBeInterrupted) return false
			if (activeBehavior.priority >= behavior.priority) return false
		}

		this.activeBehavior = behavior
		clearRoute()
		return true
	}

	fun stopBehavior(behavior: HoppingSpiderBehavior) {
		if (activeBehavior !== behavior) return
		activeBehavior = null
		clearRoute()
	}

	fun cancelBehaviorAndReturnToNest(level: ServerLevel, nestPosition: Vec3) {
		if (!carriedStack.isEmpty) {
			dropCarriedItem(level)
		}

		activeBehavior = null
		position = nestPosition
		clearRoute()
	}

	fun travelTo(
		level: ServerLevel,
		currentNodeUuid: UUID,
		targetNodeUuid: UUID
	): TravelResult {
		val savedData = WebSavedData.get(level)
		val currentNode = savedData.getNode(currentNodeUuid) ?: return TravelResult.MISSING_PATH
		val targetNode = savedData.getNode(targetNodeUuid) ?: return TravelResult.MISSING_PATH
		var path: WebPath? = null

		for (line in currentNode.lines) {
			val network = savedData.getNetwork(line.uuid) ?: continue
			path = network.findShortestPath(currentNode, targetNode) ?: continue
			break
		}

		val resolvedPath = path ?: return TravelResult.MISSING_PATH
		val routeChanged = updateRoute(level, targetNodeUuid, resolvedPath)
		if (!moveAlong(resolvedPath)) {
			return if (routeChanged) TravelResult.STARTED else TravelResult.TRAVELLING
		}

		return TravelResult.ARRIVED
	}

	private fun updateRoute(
		level: ServerLevel,
		targetNodeUuid: UUID,
		path: WebPath
	): Boolean {
		val newRoute = HoppingSpiderRoute.fromPath(path, level.gameTime, travelSpeed)
		val route = route
		if (route != null && route.matches(targetNodeUuid, newRoute.positions)) return false

		this.route = newRoute
		routeProgress = 0.0
		position = newRoute.positions.first()
		return true
	}

	private fun moveAlong(path: WebPath): Boolean {
		routeProgress += travelSpeed
		position = getPathPosition(path, routeProgress)
		return routeProgress >= path.distance
	}

	private fun getPathPosition(path: WebPath, progress: Double): Vec3 {
		if (path.segments.isEmpty()) return path.endNode.position

		var remainingDistance = progress
		for (segment in path.segments) {
			if (remainingDistance > segment.distance) {
				remainingDistance -= segment.distance
				continue
			}

			val segmentProgress = remainingDistance / segment.distance
			return segment.fromNode.position.lerp(segment.toNode.position, segmentProgress)
		}

		return path.endNode.position
	}

	fun completeTransportLeg(
		level: ServerLevel,
		transportBehavior: HoppingSpiderTransportBehavior,
		nestPosition: Vec3
	): Boolean {
		return when (transportBehavior.phase) {
			HoppingSpiderTransportBehavior.Phase.TO_SOURCE -> {
				pickUpItem(level, transportBehavior)
				true
			}

			HoppingSpiderTransportBehavior.Phase.TO_DESTINATION -> deliverItem(level, transportBehavior)

			HoppingSpiderTransportBehavior.Phase.RETURNING_ITEM -> {
				returnItem(level, transportBehavior)
				true
			}

			HoppingSpiderTransportBehavior.Phase.RETURNING -> {
				finishTransport(nestPosition)
				true
			}

			HoppingSpiderTransportBehavior.Phase.RETURNING_FROM_SOURCE -> {
				finishTransport(nestPosition)
				true
			}
		}
	}

	private fun pickUpItem(level: ServerLevel, transportBehavior: HoppingSpiderTransportBehavior) {
		val source = getBlockAnchor(level, transportBehavior.sourceNodeUuid)
		if (source == null) {
			cancelTransport()
			return
		}

		val webPort = WebPortItem.getComponent(source.webPort)

		if (webPort.transferDirection != WebPortComponent.TransferDirection.INPUT) {
			cancelTransport()
			return
		}

		val handler = getItemHandler(level, source)
		if (handler == null) {
			cancelTransport()
			return
		}

		val filter = webPort.getFilter()
		val stack = handler.extractItem(transportBehavior.sourceSlot, transportBehavior.transferAmount, true)

		if (stack.isEmpty) {
			cancelTransport()
			return
		}

		if (!filter.isEmpty && !ItemFilterItem.passesFilter(filter, stack)) {
			cancelTransport()
			return
		}

		val extracted = handler.extractItem(transportBehavior.sourceSlot, transportBehavior.transferAmount, false)
		if (extracted.isEmpty) {
			cancelTransport()
			return
		}

		carriedStack = extracted
		startNextLeg(HoppingSpiderTransportBehavior.Phase.TO_DESTINATION)
	}

	private fun deliverItem(level: ServerLevel, transportBehavior: HoppingSpiderTransportBehavior): Boolean {
		val destination = getBlockAnchor(level, transportBehavior.destinationNodeUuid)
			?: return failDelivery(level, transportBehavior, HoppingSpiderTransportBehavior.FailureReason.DESTINATION_MISSING)

		val destinationWebPort = WebPortItem.getComponent(destination.webPort)

		if (destinationWebPort.transferDirection != WebPortComponent.TransferDirection.OUTPUT) {
			return failDelivery(level, transportBehavior, HoppingSpiderTransportBehavior.FailureReason.DESTINATION_NOT_OUTPUT)
		}

		val source = getBlockAnchor(level, transportBehavior.sourceNodeUuid)
			?: return failDelivery(level, transportBehavior, HoppingSpiderTransportBehavior.FailureReason.SOURCE_MISSING)

		val sourceWebPort = WebPortItem.getComponent(source.webPort)

		if (sourceWebPort.color != destinationWebPort.color) {
			return failDelivery(level, transportBehavior, HoppingSpiderTransportBehavior.FailureReason.CHANNEL_CHANGED)
		}

		val filter = destinationWebPort.getFilter()
		if (!filter.isEmpty && !ItemFilterItem.passesFilter(filter, carriedStack)) {
			return failDelivery(level, transportBehavior, HoppingSpiderTransportBehavior.FailureReason.FILTER_CHANGED)
		}

		val handler = getItemHandler(level, destination)
			?: return failDelivery(level, transportBehavior, HoppingSpiderTransportBehavior.FailureReason.DESTINATION_UNAVAILABLE)

		carriedStack = insertItem(handler, carriedStack)
		if (!carriedStack.isEmpty) {
			return failDelivery(level, transportBehavior, HoppingSpiderTransportBehavior.FailureReason.DESTINATION_FULL)
		}

		transportBehavior.failureReason = null
		startNextLeg(HoppingSpiderTransportBehavior.Phase.RETURNING)

		return true
	}

	private fun failDelivery(
		level: ServerLevel,
		transportBehavior: HoppingSpiderTransportBehavior,
		reason: HoppingSpiderTransportBehavior.FailureReason
	): Boolean {
		val reasonChanged = transportBehavior.failureReason != reason
		transportBehavior.failureReason = reason

		if (reason.shouldRetry) return reasonChanged

		if (reason == HoppingSpiderTransportBehavior.FailureReason.DESTINATION_MISSING
			|| reason == HoppingSpiderTransportBehavior.FailureReason.SOURCE_MISSING
		) {
			dropCarriedItem(level)
			cancelTransport()
			return true
		}

		startNextLeg(HoppingSpiderTransportBehavior.Phase.RETURNING_ITEM)

		return true
	}

	private fun returnItem(level: ServerLevel, transportBehavior: HoppingSpiderTransportBehavior) {
		val source = getBlockAnchor(level, transportBehavior.sourceNodeUuid)
		if (source == null) {
			dropCarriedItem(level)
			cancelTransport()
			return
		}

		val handler = getItemHandler(level, source)
		if (handler == null) {
			dropCarriedItem(level)
			startNextLeg(HoppingSpiderTransportBehavior.Phase.RETURNING_FROM_SOURCE)
			return
		}

		carriedStack = returnToSource(handler, transportBehavior.sourceSlot, carriedStack)

		if (!carriedStack.isEmpty) {
			dropCarriedItem(level)
		}

		startNextLeg(HoppingSpiderTransportBehavior.Phase.RETURNING_FROM_SOURCE)
	}

	private fun returnToSource(handler: IItemHandler, sourceSlot: Int, stack: ItemStack): ItemStack {
		var remainder = stack

		if (sourceSlot in 0 until handler.slots) {
			remainder = handler.insertItem(sourceSlot, remainder, false)
		}

		for (slot in 0 until handler.slots) {
			if (slot == sourceSlot) continue

			remainder = handler.insertItem(slot, remainder, false)
			if (remainder.isEmpty) break
		}

		return remainder
	}

	fun handleMissingTransportPath(level: ServerLevel): Boolean {
		if (!carriedStack.isEmpty) dropCarriedItem(level)
		cancelTransport()

		return true
	}

	private fun dropCarriedItem(level: ServerLevel) {
		val position = position ?: return

		Containers.dropItemStack(
			level,
			position.x,
			position.y,
			position.z,
			carriedStack
		)

		carriedStack = ItemStack.EMPTY
	}

	private fun startNextLeg(phase: HoppingSpiderTransportBehavior.Phase) {
		val transportBehavior = transportBehavior ?: return
		transportBehavior.phase = phase
		route = null
		routeProgress = 0.0
	}

	private fun finishTransport(nestPosition: Vec3) {
		transportBehavior = null
		position = nestPosition
		route = null
		routeProgress = 0.0
	}

	private fun cancelTransport() {
		activeBehavior = null
		route = null
		routeProgress = 0.0
	}

	fun clearRoute() {
		route = null
		routeProgress = 0.0
	}

	private fun getBlockAnchor(level: ServerLevel, uuid: UUID): WebBlockAnchor? {
		return WebSavedData.get(level).getNode(uuid) as? WebBlockAnchor
	}

	private fun getItemHandler(level: ServerLevel, anchor: WebBlockAnchor): IItemHandler? {
		if (!anchor.hasWebPort) return null
		return level.getCapability(Capabilities.ItemHandler.BLOCK, anchor.blockPos, anchor.face)
	}

	private fun insertItem(handler: IItemHandler, stack: ItemStack): ItemStack {
		var remainder = stack
		for (slot in 0 until handler.slots) {
			remainder = handler.insertItem(slot, remainder, false)
			if (remainder.isEmpty) break
		}

		return remainder
	}

	fun getRenderPosition(gameTime: Long, partialTick: Float): Vec3? {
		return route?.getPosition(gameTime, partialTick) ?: position
	}

	fun save(registries: HolderLookup.Provider): CompoundTag {
		val tag = CompoundTag()
		tag.putUUID(UUID_TAG, uuid)
		tag.putDouble(ROUTE_PROGRESS_TAG, routeProgress)

		if (customName != null) {
			tag.putString(CUSTOM_NAME_TAG, customName)
		}

		val activeBehavior = activeBehavior
		if (activeBehavior != null) {
			tag.put(ACTIVE_BEHAVIOR_TAG, activeBehavior.save())
		}

		val route = route
		if (route != null) {
			tag.put(ROUTE_TAG, route.save())
		}

		if (!carriedStack.isEmpty) {
			tag.put(CARRIED_STACK_TAG, carriedStack.save(registries))
		}

		val position = position
		if (position != null) {
			tag.putDouble(POSITION_X_TAG, position.x)
			tag.putDouble(POSITION_Y_TAG, position.y)
			tag.putDouble(POSITION_Z_TAG, position.z)
		}

		return tag
	}

	private val travelSpeed: Double
		get() {
			val normalizedVariation = ((uuid.leastSignificantBits and 0xffffL).toDouble() / 0xffff) * 2.0 - 1.0
			return BASE_TRAVEL_SPEED * (1.0 + normalizedVariation * SPEED_VARIATION)
		}

	enum class TravelResult {
		STARTED,
		TRAVELLING,
		ARRIVED,
		MISSING_PATH
	}

	companion object {
		private const val UUID_TAG = "Uuid"
		private const val CUSTOM_NAME_TAG = "CustomName"
		private const val ACTIVE_BEHAVIOR_TAG = "ActiveBehavior"
		private const val ROUTE_TAG = "Route"
		private const val ROUTE_PROGRESS_TAG = "RouteProgress"
		private const val CARRIED_STACK_TAG = "CarriedStack"
		private const val POSITION_X_TAG = "PositionX"
		private const val POSITION_Y_TAG = "PositionY"
		private const val POSITION_Z_TAG = "PositionZ"
		private const val BASE_TRAVEL_SPEED = 0.15
		private const val SPEED_VARIATION = 0.1

		fun load(tag: CompoundTag, registries: HolderLookup.Provider): HoppingSpider {
			val uuid = getUuid(tag)
			val customName = if (tag.contains(CUSTOM_NAME_TAG)) tag.getString(CUSTOM_NAME_TAG) else null
			val spider = HoppingSpider(uuid, customName)
			spider.activeBehavior = getActiveBehavior(tag)
			spider.route = getRoute(tag)
			spider.routeProgress = tag.getDouble(ROUTE_PROGRESS_TAG)
			spider.carriedStack = getCarriedStack(tag, registries)
			spider.position = getPosition(tag)
			return spider
		}

		private fun getUuid(tag: CompoundTag): UUID {
			if (tag.hasUUID(UUID_TAG)) return tag.getUUID(UUID_TAG)
			return UUID.randomUUID()
		}

		private fun getActiveBehavior(tag: CompoundTag): HoppingSpiderBehavior? {
			if (!tag.contains(ACTIVE_BEHAVIOR_TAG)) return null

			val behaviorTag = tag.getCompound(ACTIVE_BEHAVIOR_TAG)
			return when (behaviorTag.getString("Type")) {
				HoppingSpiderTransportBehavior.TYPE -> HoppingSpiderTransportBehavior.load(behaviorTag)
				HoppingSpiderWanderBehavior.TYPE -> HoppingSpiderWanderBehavior.load(behaviorTag)
				else -> null
			}
		}

		private fun getRoute(tag: CompoundTag): HoppingSpiderRoute? {
			if (!tag.contains(ROUTE_TAG)) return null
			return HoppingSpiderRoute.load(tag.getCompound(ROUTE_TAG))
		}

		private fun getCarriedStack(
			tag: CompoundTag,
			registries: HolderLookup.Provider
		): ItemStack {
			return ItemStack.parseOptional(registries, tag.getCompound(CARRIED_STACK_TAG))
		}

		private fun getPosition(tag: CompoundTag): Vec3? {
			if (!tag.contains(POSITION_X_TAG)) return null

			return Vec3(
				tag.getDouble(POSITION_X_TAG),
				tag.getDouble(POSITION_Y_TAG),
				tag.getDouble(POSITION_Z_TAG)
			)
		}
	}
}