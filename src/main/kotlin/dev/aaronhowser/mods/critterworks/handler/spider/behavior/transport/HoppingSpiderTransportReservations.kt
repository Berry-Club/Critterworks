package dev.aaronhowser.mods.critterworks.handler.spider.behavior.transport

import java.util.*

class HoppingSpiderTransportReservations {

	private val sources = mutableSetOf<Pair<UUID, Int>>()
	private val destinations = mutableSetOf<UUID>()

	fun reserve(behavior: HoppingSpiderTransportBehavior) {
		sources.add(behavior.sourceNodeUuid to behavior.sourceSlot)

		destinations.add(behavior.destinationNodeUuid)
	}

	fun isSourceReserved(nodeUuid: UUID, slot: Int): Boolean {
		return nodeUuid to slot in sources
	}

	fun isDestinationReserved(nodeUuid: UUID): Boolean {
		return nodeUuid in destinations
	}
}