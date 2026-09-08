package dev.aaronhowser.mods.critterworks.event

import dev.aaronhowser.mods.critterworks.Critterworks
import dev.aaronhowser.mods.critterworks.client.render.bewlr.CritterCageItemRenderer
import dev.aaronhowser.mods.critterworks.client.render.block_entity.CritterCageBlockRenderer
import dev.aaronhowser.mods.critterworks.client.render.block_entity.HoppingSpiderNestBlockRenderer
import dev.aaronhowser.mods.critterworks.client.render.entity.ScoochwormPartRenderer
import dev.aaronhowser.mods.critterworks.client.render.entity.ScoochwormRenderer
import dev.aaronhowser.mods.critterworks.client.render.item.ScoochwormGpsItemRenderer
import dev.aaronhowser.mods.critterworks.handler.web.line.ClientWebLineInteractionHandler
import dev.aaronhowser.mods.critterworks.handler.web.line.ClientWebLines
import dev.aaronhowser.mods.critterworks.registry.ModBlockEntityTypes
import dev.aaronhowser.mods.critterworks.registry.ModEntityTypes
import dev.aaronhowser.mods.critterworks.registry.ModItems
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent
import net.neoforged.neoforge.client.event.EntityRenderersEvent
import net.neoforged.neoforge.client.event.InputEvent
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent

@EventBusSubscriber(
	modid = Critterworks.MOD_ID,
	value = [Dist.CLIENT]
)
object ClientEvents {
	@SubscribeEvent
	fun interactWithWebLine(event: InputEvent.InteractionKeyMappingTriggered) {
		ClientWebLineInteractionHandler.handleInteraction(event)
	}

	@SubscribeEvent
	fun onLoggingOut(event: ClientPlayerNetworkEvent.LoggingOut) {
		ClientWebLines.clear()
	}

	@SubscribeEvent
	fun registerEntityRenderers(event: EntityRenderersEvent.RegisterRenderers) {
		event.registerBlockEntityRenderer(ModBlockEntityTypes.CRITTER_CAGE.get(), ::CritterCageBlockRenderer)
		event.registerBlockEntityRenderer(
			ModBlockEntityTypes.HOPPING_SPIDER_NEST.get(),
			::HoppingSpiderNestBlockRenderer
		)
		event.registerEntityRenderer(ModEntityTypes.SCOOCHWORM.get(), ::ScoochwormRenderer)
		event.registerEntityRenderer(ModEntityTypes.SCOOCHWORM_PART.get(), ::ScoochwormPartRenderer)
	}

	@SubscribeEvent
	fun registerClientExtensions(event: RegisterClientExtensionsEvent) {
		event.registerItem(
			CritterCageItemRenderer.ClientItemExtensions,
			ModItems.CRITTER_CAGE.get()
		)
		event.registerItem(
			ScoochwormGpsItemRenderer.ClientItemExtensions,
			ModItems.SCOOCHWORM_GPS.get()
		)
	}

}