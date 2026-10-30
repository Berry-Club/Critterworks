package dev.aaronhowser.mods.critterworks.event

import dev.aaronhowser.mods.critterworks.Critterworks
import dev.aaronhowser.mods.critterworks.event.custom.WebLineInteractionEvent
import dev.aaronhowser.mods.critterworks.event.custom.WebNodeInteractionEvent
import dev.aaronhowser.mods.critterworks.handler.web.WebLineType
import dev.aaronhowser.mods.critterworks.handler.web.item.ItemWebInteractionHandler
import dev.aaronhowser.mods.critterworks.handler.web.redstone.interaction.RedstoneWebInteractionHandler
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber

@EventBusSubscriber(modid = Critterworks.MOD_ID)
object WebInteractionEvents {

	@SubscribeEvent
	fun handleNodeInteraction(event: WebNodeInteractionEvent) {
		if (event.isCanceled) return

		when (event.webType) {
			WebLineType.ITEM -> ItemWebInteractionHandler.handleNodeInteraction(event)
			WebLineType.REDSTONE -> RedstoneWebInteractionHandler.handleNodeInteraction(event)
		}
	}

	@SubscribeEvent
	fun handleLineInteraction(event: WebLineInteractionEvent) {
		if (event.isCanceled) return

		when (event.webType) {
			WebLineType.ITEM -> ItemWebInteractionHandler.handleLineInteraction(event)
			WebLineType.REDSTONE -> RedstoneWebInteractionHandler.handleLineInteraction(event)
		}
	}
}