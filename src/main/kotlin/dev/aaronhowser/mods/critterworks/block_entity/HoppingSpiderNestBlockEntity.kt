package dev.aaronhowser.mods.critterworks.block_entity

import dev.aaronhowser.mods.aaron.block_entity.SyncingBlockEntity
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isTrue
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.toComponent
import dev.aaronhowser.mods.critterworks.datagen.language.ModMenuLang
import dev.aaronhowser.mods.critterworks.handler.spider.HoppingSpider
import dev.aaronhowser.mods.critterworks.handler.spider.behavior.HoppingSpiderWanderBehavior
import dev.aaronhowser.mods.critterworks.handler.spider.behavior.transport.HoppingSpiderTransportBehavior
import dev.aaronhowser.mods.critterworks.handler.spider.behavior.transport.HoppingSpiderTransportCandidate
import dev.aaronhowser.mods.critterworks.handler.spider.behavior.transport.HoppingSpiderTransportReservations
import dev.aaronhowser.mods.critterworks.handler.web.WebNetwork
import dev.aaronhowser.mods.critterworks.handler.web.WebSavedData
import dev.aaronhowser.mods.critterworks.handler.web.node.WebBlockAnchor
import dev.aaronhowser.mods.critterworks.handler.web.node.WebNode
import dev.aaronhowser.mods.critterworks.item.HoppingSpiderItem
import dev.aaronhowser.mods.critterworks.item.ItemFilterItem
import dev.aaronhowser.mods.critterworks.item.WebPortItem
import dev.aaronhowser.mods.critterworks.item.component.WebPortComponent
import dev.aaronhowser.mods.critterworks.menu.spider_nest.SpiderNestMenu
import dev.aaronhowser.mods.critterworks.registry.ModBlockEntityTypes
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.MenuProvider
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.items.IItemHandler

class HoppingSpiderNestBlockEntity(
	pos: BlockPos,
	state: BlockState
) : SyncingBlockEntity(ModBlockEntityTypes.HOPPING_SPIDER_NEST.get(), pos, state), MenuProvider {

	override val syncImmediately: Boolean = false

	val hoppingSpiders: MutableList<HoppingSpider> = mutableListOf()

	fun addSpider(stack: ItemStack): Boolean {
		if (hoppingSpiders.size >= MAX_SPIDERS) return false

		hoppingSpiders.add(HoppingSpiderItem.createSpider(stack))
		setChangedAndSync()
		return true
	}

	fun removeLastSpider(): HoppingSpider? {
		if (hoppingSpiders.isEmpty()) return null

		val spider = hoppingSpiders.removeLast()
		setChangedAndSync()
		return spider
	}

	private fun setChangedAndSync() {
		setChanged()

		val level = level ?: return
		level.sendBlockUpdated(blockPos, blockState, blockState, Block.UPDATE_CLIENTS)
	}

	override fun getDisplayName(): Component {
		return ModMenuLang.SPIDER_NEST_TITLE.toComponent()
	}

	override fun createMenu(
		containerId: Int,
		playerInventory: Inventory,
		player: Player
	): AbstractContainerMenu {
		return SpiderNestMenu(containerId, playerInventory, this)
	}

	private fun serverTick(level: ServerLevel) {
		var shouldSync = assignTransportBehaviors(level)

		for (spider in hoppingSpiders) {
			if (spider.activeBehavior == null) {
				startWandering(level, spider)
			}

			if (spider.serverTick(level, blockPos.center)) {
				shouldSync = true
			}
		}

		if (assignTransportBehaviors(level)) {
			shouldSync = true
		}

		if (shouldSync || hasActiveBehaviors()) {
			setChanged()
		}

		if (shouldSync) {
			level.sendBlockUpdated(blockPos, blockState, blockState, Block.UPDATE_CLIENTS)
		}
	}

	private fun hasActiveBehaviors(): Boolean {
		for (spider in hoppingSpiders) {
			if (spider.activeBehavior != null) return true
		}

		return false
	}

	private fun assignTransportBehaviors(level: ServerLevel): Boolean {
		val reservations = getTransportReservations(level)
		var assignedBehavior = false

		while (true) {
			var bestCandidate: HoppingSpiderTransportCandidate? = null

			for (spider in hoppingSpiders) {
				val candidate = findTransportCandidate(level, reservations, spider) ?: continue

				if (candidate.isPreferredOver(bestCandidate)) {
					bestCandidate = candidate
				}
			}

			val candidate = bestCandidate ?: break
			if (!assignTransportBehavior(candidate.spider, candidate.behavior)) break
			reservations.reserve(candidate.behavior)

			assignedBehavior = true
		}

		return assignedBehavior
	}

	private fun getTransportReservations(level: ServerLevel): HoppingSpiderTransportReservations {
		val reservations = HoppingSpiderTransportReservations()
		val nestPositions = mutableSetOf(blockPos)
		val savedData = WebSavedData.get(level)

		for (network in savedData.getNetworksAt(blockPos)) {
			for (node in network.getNodes()) {
				if (node !is WebBlockAnchor) continue
				if (level.getBlockEntity(node.blockPos) !is HoppingSpiderNestBlockEntity) continue

				nestPositions.add(node.blockPos)
			}
		}

		for (pos in nestPositions) {
			val nest = level.getBlockEntity(pos) as? HoppingSpiderNestBlockEntity ?: continue

			for (spider in nest.hoppingSpiders) {
				val transportBehavior = spider.transportBehavior ?: continue
				if (transportBehavior.phase == HoppingSpiderTransportBehavior.Phase.RETURNING) continue
				if (transportBehavior.phase == HoppingSpiderTransportBehavior.Phase.RETURNING_FROM_SOURCE) continue

				reservations.reserve(transportBehavior)
			}
		}

		return reservations
	}

	private fun assignTransportBehavior(spider: HoppingSpider, transportBehavior: HoppingSpiderTransportBehavior): Boolean {
		return spider.tryStartBehavior(transportBehavior)
	}

	private fun startWandering(level: ServerLevel, spider: HoppingSpider) {
		for (network in WebSavedData.get(level).getNetworksAt(blockPos)) {
			val homeNode = getAnchors(network, blockPos).firstOrNull() ?: continue
			spider.tryStartBehavior(HoppingSpiderWanderBehavior(homeNode.uuid))
			spider.position = homeNode.position
			return
		}
	}

	private fun getCurrentNode(level: ServerLevel, spider: HoppingSpider): WebNode? {
		val activeBehavior = spider.activeBehavior
		if (activeBehavior != null && !activeBehavior.canBeInterrupted) return null

		val transportBehavior = spider.transportBehavior
		if (transportBehavior != null) return null

		val currentNodeUuid = activeBehavior?.currentNodeUuid ?: return null
		return WebSavedData.get(level).getNode(currentNodeUuid)
	}

	private fun findTransportCandidate(
		level: ServerLevel,
		reservations: HoppingSpiderTransportReservations,
		spider: HoppingSpider
	): HoppingSpiderTransportCandidate? {
		val activeBehavior = spider.activeBehavior
		if (activeBehavior != null && !activeBehavior.canBeInterrupted) return null
		if (spider.transportBehavior != null) return null

		val savedData = WebSavedData.get(level)
		val currentNode = getCurrentNode(level, spider)
		var bestCandidate: HoppingSpiderTransportCandidate? = null

		for (network in savedData.getNetworksAt(blockPos)) {
			val nestNodes = getAnchors(network, blockPos)
			val startingNode = if (currentNode == null || isNestNode(currentNode)) {
				nestNodes.firstOrNull() ?: continue
			} else {
				currentNode
			}

			val candidate = findTransportBehaviorInNetwork(level, network, reservations, spider, startingNode)

			if (candidate?.isPreferredOver(bestCandidate) == true) {
				bestCandidate = candidate
			}
		}

		return bestCandidate
	}

	private fun isNestNode(node: WebNode): Boolean {
		return node is WebBlockAnchor && node.blockPos == blockPos
	}

	private fun findTransportBehaviorInNetwork(
		level: ServerLevel,
		network: WebNetwork,
		reservations: HoppingSpiderTransportReservations,
		spider: HoppingSpider,
		startingNode: WebNode
	): HoppingSpiderTransportCandidate? {
		val nestNodes = getAnchors(network, blockPos)
		val inventoryNodes = getInventoryAnchors(level, network)
		var bestCandidate: HoppingSpiderTransportCandidate? = null

		for (sourceNode in inventoryNodes) {
			val candidate = findTransportBehaviorFromSource(
				level,
				network,
				nestNodes,
				inventoryNodes,
				sourceNode,
				reservations,
				spider,
				startingNode
			)

			if (candidate?.isPreferredOver(bestCandidate).isTrue()) {
				bestCandidate = candidate
			}
		}

		return bestCandidate
	}

	private fun findTransportBehaviorFromSource(
		level: ServerLevel,
		network: WebNetwork,
		nestNodes: List<WebBlockAnchor>,
		inventoryNodes: List<WebBlockAnchor>,
		sourceNode: WebBlockAnchor,
		reservations: HoppingSpiderTransportReservations,
		spider: HoppingSpider,
		startingNode: WebNode
	): HoppingSpiderTransportCandidate? {
		val sourceWebPort = WebPortItem.getComponent(sourceNode.webPort)
		if (sourceWebPort.transferDirection != WebPortComponent.TransferDirection.INPUT) return null

		val sourceHandler = getItemHandler(level, sourceNode) ?: return null
		var bestCandidate: HoppingSpiderTransportCandidate? = null

		for (sourceSlot in 0 until sourceHandler.slots) {
			if (reservations.isSourceReserved(sourceNode.uuid, sourceSlot)) continue

			val stack = sourceHandler.extractItem(sourceSlot, MAX_TRANSFER_SIZE, true)

			if (stack.isEmpty) continue
			if (!passesFilter(sourceWebPort, stack)) continue

			val candidate = findDestinationTransportBehavior(
				level,
				network,
				nestNodes,
				inventoryNodes,
				sourceNode,
				sourceSlot,
				stack,
				reservations,
				spider,
				startingNode
			)

			if (candidate?.isPreferredOver(bestCandidate) == true) {
				bestCandidate = candidate
			}
		}

		return bestCandidate
	}

	private fun findDestinationTransportBehavior(
		level: ServerLevel,
		network: WebNetwork,
		nestNodes: List<WebBlockAnchor>,
		inventoryNodes: List<WebBlockAnchor>,
		sourceNode: WebBlockAnchor,
		sourceSlot: Int,
		stack: ItemStack,
		reservations: HoppingSpiderTransportReservations,
		spider: HoppingSpider,
		startingNode: WebNode
	): HoppingSpiderTransportCandidate? {
		val sourceWebPort = WebPortItem.getComponent(sourceNode.webPort)
		var bestCandidate: HoppingSpiderTransportCandidate? = null

		for (destinationNode in inventoryNodes) {
			if (isSameFace(sourceNode, destinationNode)) continue

			val destinationWebPort = WebPortItem.getComponent(destinationNode.webPort)

			if (destinationWebPort.transferDirection != WebPortComponent.TransferDirection.OUTPUT) continue
			if (destinationWebPort.color != sourceWebPort.color) continue
			if (!passesFilter(destinationWebPort, stack)) continue
			if (reservations.isDestinationReserved(destinationNode.uuid)) continue
			if (network.findShortestPath(sourceNode, destinationNode) == null) continue

			val transferAmount = getInsertableAmount(level, destinationNode, stack)
			if (transferAmount == 0) continue

			val pathToSource = network.findShortestPath(startingNode, sourceNode) ?: continue

			val homeNode = findHomeNode(network, nestNodes, destinationNode)
				?: continue

			val transportBehavior = HoppingSpiderTransportBehavior(
				homeNode.uuid,
				sourceNode.uuid,
				destinationNode.uuid,
				sourceSlot,
				transferAmount,
				startingNode.uuid
			)

			val candidate = HoppingSpiderTransportCandidate(
				spider,
				transportBehavior,
				sourceWebPort.priority,
				destinationWebPort.priority,
				pathToSource.distance,
				transferAmount
			)

			if (candidate.isPreferredOver(bestCandidate)) {
				bestCandidate = candidate
			}
		}

		return bestCandidate
	}

	private fun passesFilter(webPortComponent: WebPortComponent, stack: ItemStack): Boolean {
		val filter = webPortComponent.getFilter()
		return filter.isEmpty || ItemFilterItem.passesFilter(filter, stack)
	}

	private fun isSameFace(first: WebBlockAnchor, second: WebBlockAnchor): Boolean {
		return first.blockPos == second.blockPos && first.face == second.face
	}

	private fun findHomeNode(
		network: WebNetwork,
		nestNodes: List<WebBlockAnchor>,
		destinationNode: WebBlockAnchor
	): WebBlockAnchor? {
		for (nestNode in nestNodes) {
			if (network.findShortestPath(destinationNode, nestNode) != null) return nestNode
		}

		return null
	}

	private fun getInventoryAnchors(level: ServerLevel, network: WebNetwork): List<WebBlockAnchor> {
		val anchors = mutableListOf<WebBlockAnchor>()

		for (node in network.getNodes()) {
			if (node !is WebBlockAnchor || node.blockPos == blockPos) continue
			if (!node.hasWebPort) continue
			if (getItemHandler(level, node) == null) continue

			anchors.add(node)
		}

		return anchors
	}

	private fun getAnchors(network: WebNetwork, pos: BlockPos): List<WebBlockAnchor> {
		val anchors = mutableListOf<WebBlockAnchor>()

		for (node in network.getNodes()) {
			if (node is WebBlockAnchor && node.blockPos == pos) {
				anchors.add(node)
			}
		}

		return anchors
	}

	private fun getItemHandler(level: ServerLevel, anchor: WebBlockAnchor): IItemHandler? {
		return level.getCapability(Capabilities.ItemHandler.BLOCK, anchor.blockPos, anchor.face)
	}

	private fun getInsertableAmount(level: ServerLevel, anchor: WebBlockAnchor, stack: ItemStack): Int {
		val handler = getItemHandler(level, anchor) ?: return 0
		var remainder = stack

		for (slot in 0 until handler.slots) {
			remainder = handler.insertItem(slot, remainder, true)
			if (remainder.isEmpty) break
		}

		return stack.count - remainder.count
	}

	override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
		super.saveAdditional(tag, registries)
		tag.put(SPIDERS_TAG, saveSpiders(registries))
	}

	private fun saveSpiders(registries: HolderLookup.Provider): ListTag {
		val spidersTag = ListTag()

		for (spider in hoppingSpiders) {
			spidersTag.add(spider.save(registries))
		}

		return spidersTag
	}

	override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
		super.loadAdditional(tag, registries)
		hoppingSpiders.clear()
		loadSpiders(tag.getList(SPIDERS_TAG, CompoundTag.TAG_COMPOUND.toInt()), registries)
	}

	private fun loadSpiders(spidersTag: ListTag, registries: HolderLookup.Provider) {
		for (index in spidersTag.indices) {
			if (hoppingSpiders.size >= MAX_SPIDERS) break
			hoppingSpiders.add(HoppingSpider.load(spidersTag.getCompound(index), registries))
		}
	}

	companion object {
		private const val SPIDERS_TAG = "HoppingSpiders"
		const val MAX_SPIDERS = 64
		private const val MAX_TRANSFER_SIZE = 64

		fun serverTick(
			level: Level,
			pos: BlockPos,
			state: BlockState,
			blockEntity: HoppingSpiderNestBlockEntity
		) {
			if (level is ServerLevel) {
				blockEntity.serverTick(level)
			}
		}
	}
}