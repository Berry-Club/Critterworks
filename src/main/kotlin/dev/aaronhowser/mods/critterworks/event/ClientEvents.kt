package dev.aaronhowser.mods.critterworks.event

import dev.aaronhowser.mods.critterworks.Critterworks
import dev.aaronhowser.mods.critterworks.client.render.block_entity.CritterCageBlockRenderer
import dev.aaronhowser.mods.critterworks.client.render.block_entity.HoppingSpiderNestBlockRenderer
import dev.aaronhowser.mods.critterworks.client.render.block_entity.SparkbugBushBlockRenderer
import dev.aaronhowser.mods.critterworks.client.render.entity.ScoochwormPartRenderer
import dev.aaronhowser.mods.critterworks.client.render.entity.ScoochwormRenderer
import dev.aaronhowser.mods.critterworks.client.render.entity.FilterfeederRenderer
import dev.aaronhowser.mods.critterworks.client.render.item.CritterCageItemRenderer
import dev.aaronhowser.mods.critterworks.client.render.item.TardigradeItemDecorator
import dev.aaronhowser.mods.critterworks.handler.web.line.ClientWebLineInteractionHandler
import dev.aaronhowser.mods.critterworks.handler.web.line.ClientWebLines
import dev.aaronhowser.mods.critterworks.particle.FireflyParticleProvider
import dev.aaronhowser.mods.critterworks.registry.ModBlockEntityTypes
import dev.aaronhowser.mods.critterworks.registry.ModEntityTypes
import dev.aaronhowser.mods.critterworks.registry.ModItems
import dev.aaronhowser.mods.critterworks.registry.ModParticleTypes
import net.minecraft.core.registries.BuiltInRegistries
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.*
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
		event.registerBlockEntityRenderer(ModBlockEntityTypes.SPARKBUG_BUSH.get(), ::SparkbugBushBlockRenderer)
		event.registerEntityRenderer(ModEntityTypes.FILTERFEEDER.get(), ::FilterfeederRenderer)
		event.registerEntityRenderer(ModEntityTypes.SCOOCHWORM.get(), ::ScoochwormRenderer)
		event.registerEntityRenderer(ModEntityTypes.SCOOCHWORM_PART.get(), ::ScoochwormPartRenderer)
	}

	@SubscribeEvent
	fun registerParticleProviders(event: RegisterParticleProvidersEvent) {
		event.registerSpriteSet(ModParticleTypes.FIREFLY.get(), ::FireflyParticleProvider)
	}

	@SubscribeEvent
	fun registerItemDecorations(event: RegisterItemDecorationsEvent) {
		for (item in BuiltInRegistries.ITEM) {
			event.register(item, TardigradeItemDecorator)
		}
	}

	@SubscribeEvent
	fun registerClientExtensions(event: RegisterClientExtensionsEvent) {
		event.registerItem(
			CritterCageItemRenderer.ClientItemExtensions,
			ModItems.CRITTER_CAGE.get()
		)
	}

}