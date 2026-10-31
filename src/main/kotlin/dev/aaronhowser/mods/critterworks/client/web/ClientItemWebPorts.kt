package dev.aaronhowser.mods.critterworks.client.web

import dev.aaronhowser.mods.critterworks.handler.web.item.ItemWebPortData
import net.minecraft.world.item.ItemStack
import java.util.*

object ClientItemWebPorts {

	private val stacksByNodeUuid = mutableMapOf<UUID, ItemStack>()

	fun get(nodeUuid: UUID): ItemStack {
		return stacksByNodeUuid[nodeUuid] ?: ItemStack.EMPTY
	}

	fun update(webPorts: List<ItemWebPortData>) {
		for (webPort in webPorts) {
			if (webPort.stack.isEmpty) {
				stacksByNodeUuid.remove(webPort.nodeUuid)
			} else {
				stacksByNodeUuid[webPort.nodeUuid] = webPort.stack
			}
		}
	}

	fun remove(nodeUuid: UUID) {
		stacksByNodeUuid.remove(nodeUuid)
	}

	fun clear() {
		stacksByNodeUuid.clear()
	}
}