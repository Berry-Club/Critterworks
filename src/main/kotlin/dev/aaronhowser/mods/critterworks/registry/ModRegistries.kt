package dev.aaronhowser.mods.critterworks.registry

import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredRegister

object ModRegistries {

	fun register(modBus: IEventBus) {
		val registries: List<DeferredRegister<*>> = listOf(
			ModScoochwormAttachmentTypes.SCOOCHWORM_ATTACHMENT_TYPE_REGISTRY,
			ModItems.ITEM_REGISTRY,
			ModBlocks.BLOCK_REGISTRY,
			ModBlockEntityTypes.BLOCK_ENTITY_REGISTRY,
			ModCreativeModeTabs.TABS_REGISTRY,
			ModEntityTypes.ENTITY_TYPE_REGISTRY,
			ModEntityDataSerializers.ENTITY_DATA_SERIALIZER_REGISTRY,
			ModSoundEvents.SOUND_EVENT_REGISTRY,
			ModDataComponents.DATA_COMPONENT_REGISTRY,
			ModMenuTypes.MENU_TYPE_REGISTRY,
			ModMobEffects.MOB_EFFECT_REGISTRY,
			ModPotions.POTION_REGISTRY,
			ModFeatures.FEATURE_REGISTRY,
			ModParticleTypes.PARTICLE_TYPE_REGISTRY
		)

		for (registry in registries) {
			registry.register(modBus)
		}
	}
}