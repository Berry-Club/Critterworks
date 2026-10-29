package dev.aaronhowser.mods.critterworks.handler.webstone

import dev.aaronhowser.mods.critterworks.handler.web.graph.WebEdge
import java.util.*

data class WebstoneLine(
	override val uuid: UUID,
	override val firstVertex: WebstoneNode,
	override val secondVertex: WebstoneNode
) : WebEdge<WebstoneNode>() {

	var signalStrength: Int = 0
		private set

	var depth: Int = UNPOWERED_DEPTH
		private set

	val isPowered: Boolean
		get() = signalStrength > 0

	fun receiveSignal(signalStrength: Int, depth: Int): Boolean {
		require(signalStrength in MINIMUM_SIGNAL_STRENGTH..MAXIMUM_SIGNAL_STRENGTH)
		require(depth >= 0)

		if (signalStrength == 0) return false
		if (depth > this.depth) return false
		if (depth == this.depth && signalStrength <= this.signalStrength) return false

		this.signalStrength = signalStrength
		this.depth = depth
		return true
	}

	fun tryPower(line: WebstoneLine): Boolean {
		if (!isConnectedTo(line)) return false
		if (!isPowered || depth >= line.depth) return false

		return line.receiveSignal(signalStrength, depth + 1)
	}

	fun clearSignal(): Boolean {
		if (!isPowered && depth == UNPOWERED_DEPTH) return false

		signalStrength = 0
		depth = UNPOWERED_DEPTH
		return true
	}

	private fun isConnectedTo(line: WebstoneLine): Boolean {
		return firstVertex.uuid == line.firstVertex.uuid
			|| firstVertex.uuid == line.secondVertex.uuid
			|| secondVertex.uuid == line.firstVertex.uuid
			|| secondVertex.uuid == line.secondVertex.uuid
	}

	companion object {
		const val MINIMUM_SIGNAL_STRENGTH = 0
		const val MAXIMUM_SIGNAL_STRENGTH = 15
		const val UNPOWERED_DEPTH = Int.MAX_VALUE
	}

}