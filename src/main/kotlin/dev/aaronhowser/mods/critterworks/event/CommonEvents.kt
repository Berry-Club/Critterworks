package dev.aaronhowser.mods.critterworks.event

import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isDamageSource
import dev.aaronhowser.mods.critterworks.Critterworks
import dev.aaronhowser.mods.critterworks.config.ServerConfig
import dev.aaronhowser.mods.critterworks.datagen.tag.ModDamageTypeTagsProvider
import dev.aaronhowser.mods.critterworks.entity.filterfeeder.FilterfeederEntity
import dev.aaronhowser.mods.critterworks.entity.scoochworm.ScoochwormEntity
import dev.aaronhowser.mods.critterworks.handler.web.WebSavedData
import dev.aaronhowser.mods.critterworks.packet.ModPacketHandler
import dev.aaronhowser.mods.critterworks.registry.ModDataComponents
import dev.aaronhowser.mods.critterworks.registry.ModEntityTypes
import dev.aaronhowser.mods.critterworks.registry.ModPotions
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.SpawnPlacementTypes
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.level.levelgen.Heightmap
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent
import net.neoforged.neoforge.event.entity.EntityInvulnerabilityCheckEvent
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent
import net.neoforged.neoforge.event.entity.item.ItemExpireEvent
import net.neoforged.neoforge.event.level.BlockEvent
import net.neoforged.neoforge.event.level.ChunkWatchEvent
import net.neoforged.neoforge.event.tick.ServerTickEvent
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent

@EventBusSubscriber(modid = Critterworks.MOD_ID)
object CommonEvents {
	@SubscribeEvent
	fun registerPayloads(event: RegisterPayloadHandlersEvent) {
		ModPacketHandler.registerPayloads(event)
	}

	@SubscribeEvent
	fun onChunkWatch(event: ChunkWatchEvent.Watch) {
		WebSavedData.get(event.player.serverLevel()).syncChunk(event.player, event.pos)
	}

	@SubscribeEvent
	fun onChunkUnwatch(event: ChunkWatchEvent.UnWatch) {
		WebSavedData.get(event.player.serverLevel()).forgetChunk(event.player, event.pos)
	}

	@SubscribeEvent
	fun onBlockBreak(event: BlockEvent.BreakEvent) {
		markWebLinesForValidation(event)
	}

	@SubscribeEvent
	fun onBlockPlace(event: BlockEvent.EntityPlaceEvent) {
		markWebLinesForValidation(event)
	}

	private fun markWebLinesForValidation(event: BlockEvent) {
		val level = event.level
		if (level !is ServerLevel) return

		WebSavedData.get(level).markForValidation(event.pos)
	}

	@SubscribeEvent
	fun onServerTick(event: ServerTickEvent.Post) {
		val level = event.server.overworld()
		WebSavedData.get(level).validateChangedChunks(level)
	}

	@SubscribeEvent
	fun registerEntityAttributes(event: EntityAttributeCreationEvent) {
		event.put(ModEntityTypes.FILTERFEEDER.get(), FilterfeederEntity.createAttributes())
		event.put(ModEntityTypes.SCOOCHWORM.get(), ScoochwormEntity.createAttributes())
	}

	@SubscribeEvent
	fun registerSpawnPlacements(event: RegisterSpawnPlacementsEvent) {
		event.register(
			ModEntityTypes.FILTERFEEDER.get(),
			SpawnPlacementTypes.IN_WATER,
			Heightmap.Types.OCEAN_FLOOR,
			FilterfeederEntity::checkSpawnRules,
			RegisterSpawnPlacementsEvent.Operation.REPLACE
		)
	}

	@SubscribeEvent
	fun registerCapabilities(event: RegisterCapabilitiesEvent) {
		event.registerEntity(
			Capabilities.ItemHandler.ENTITY,
			ModEntityTypes.SCOOCHWORM_PART.get()
		) { bodyPart, _ -> bodyPart.getItemHandler() }

		event.registerEntity(
			Capabilities.ItemHandler.ENTITY_AUTOMATION,
			ModEntityTypes.SCOOCHWORM_PART.get()
		) { bodyPart, _ -> bodyPart.getItemHandler() }
	}

	@SubscribeEvent
	fun registerBrewingRecipes(event: RegisterBrewingRecipesEvent) {
		ModPotions.registerRecipes(event)
	}

	@SubscribeEvent
	fun onEntityInvulnerabilityCheck(event: EntityInvulnerabilityCheckEvent) {
		val entity = event.entity
		if (entity is ItemEntity
			&& event.source.isDamageSource(ModDamageTypeTagsProvider.TARDIGRADE_PROTECTS_FROM)
			&& entity.item.has(ModDataComponents.HAS_TARDIGRADE)
		) {
			event.isInvulnerable = true
		}
	}

	@SubscribeEvent
	fun onItemExpire(event: ItemExpireEvent) {
		if (ServerConfig.CONFIG.tardigradeDisablesExpiring.get()
			&& event.entity.item.has(ModDataComponents.HAS_TARDIGRADE)
		) {
			event.entity.setUnlimitedLifetime()
		}
	}

}