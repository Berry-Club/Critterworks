package dev.aaronhowser.mods.critterworks.handler.web.redstone

object RedstoneWebPowerSuppression {

	private val suppressionDepth: ThreadLocal<Int> = ThreadLocal.withInitial { 0 }

	fun begin() {
		suppressionDepth.set(suppressionDepth.get() + 1)
	}

	fun end() {
		val newDepth = suppressionDepth.get() - 1
		if (newDepth <= 0) {
			suppressionDepth.remove()
		} else {
			suppressionDepth.set(newDepth)
		}
	}

	fun isSuppressed(): Boolean {
		return suppressionDepth.get() > 0
	}
}