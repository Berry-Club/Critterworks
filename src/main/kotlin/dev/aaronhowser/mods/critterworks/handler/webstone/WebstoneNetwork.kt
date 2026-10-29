package dev.aaronhowser.mods.critterworks.handler.webstone

import kotlin.collections.ArrayDeque

class WebstoneNetwork {

	val lines: Set<WebstoneLine>
		field = mutableSetOf()

	fun addLine(line: WebstoneLine) {
		if (!lines.add(line)) return

		line.firstVertex.addLine(line)
		line.secondVertex.addLine(line)
	}

	fun removeLine(line: WebstoneLine) {
		if (!lines.remove(line)) return

		line.firstVertex.removeLine(line)
		line.secondVertex.removeLine(line)
	}

	fun recalculateSignals(sourceStrengths: Map<WebstoneLine, Int>) {
		for (line in lines) {
			line.clearSignal()
		}

		val pendingLines: ArrayDeque<WebstoneLine> = ArrayDeque()
		for ((sourceLine, signalStrength) in sourceStrengths) {
			if (sourceLine !in lines) continue
			if (!sourceLine.receiveSignal(signalStrength, 0)) continue

			pendingLines.addLast(sourceLine)
		}

		while (pendingLines.isNotEmpty()) {
			val line = pendingLines.removeFirst()
			propagateFrom(line, line.firstVertex, pendingLines)
			propagateFrom(line, line.secondVertex, pendingLines)
		}
	}

	private fun propagateFrom(
		line: WebstoneLine,
		node: WebstoneNode,
		pendingLines: ArrayDeque<WebstoneLine>
	) {
		for (connectedLine in node.lines) {
			if (connectedLine === line || connectedLine !in lines) continue
			if (!line.tryPower(connectedLine)) continue

			pendingLines.addLast(connectedLine)
		}
	}

}