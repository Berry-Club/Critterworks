package dev.aaronhowser.mods.critterworks.event

import dev.aaronhowser.mods.aaron.client.AaronClientUtil
import dev.aaronhowser.mods.critterworks.Critterworks
import dev.aaronhowser.mods.critterworks.client.block_entity.critter_cage.CritterCageBlockRenderer
import dev.aaronhowser.mods.critterworks.client.block_entity.hopping_spider_nest.HoppingSpiderNestBlockRenderer
import dev.aaronhowser.mods.critterworks.client.block_entity.sparkbug_bush.SparkbugBushBlockRenderer
import dev.aaronhowser.mods.critterworks.client.entity.filterfeeder.FilterfeederRenderer
import dev.aaronhowser.mods.critterworks.client.entity.punchprawn.PunchprawnRenderer
import dev.aaronhowser.mods.critterworks.client.entity.punchprawn.hat.PunchprawnHatLayer
import dev.aaronhowser.mods.critterworks.client.entity.scoochworm.ScoochwormPartRenderer
import dev.aaronhowser.mods.critterworks.client.entity.scoochworm.ScoochwormRenderer
import dev.aaronhowser.mods.critterworks.client.entity.snail_shield.SnailShieldRenderer
import dev.aaronhowser.mods.critterworks.client.entity.sproutsnail.SproutsnailRenderer
import dev.aaronhowser.mods.critterworks.client.item.critter_cage.CritterCageItemRenderer
import dev.aaronhowser.mods.critterworks.client.item.tardigrade.TardigradeItemDecorator
import dev.aaronhowser.mods.critterworks.handler.snail.ClientEvilSnails
import dev.aaronhowser.mods.critterworks.client.web.ClientWebLineInteractionHandler
import dev.aaronhowser.mods.critterworks.client.web.ClientWebLines
import dev.aaronhowser.mods.critterworks.item.CritterCageItem
import dev.aaronhowser.mods.critterworks.item.SnailShieldItem
import dev.aaronhowser.mods.critterworks.packet.client_to_server.SnailShieldMovedPacket
import dev.aaronhowser.mods.critterworks.particle.FireflyParticleProvider
import dev.aaronhowser.mods.critterworks.registry.*
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.entity.player.PlayerRenderer
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.util.Mth
import net.minecraft.world.entity.ai.attributes.Attributes
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
	fun renderShelledPlayer(event: RenderPlayerEvent.Pre) {
		val player = event.entity
		if (!player.getData(ModAttachmentTypes.SNAIL_SHIELD_STATE).isShelled) return
		if (!SnailShieldRenderer.render(event)) return

		event.isCanceled = true
	}

	@SubscribeEvent
	fun hideShelledPlayerHands(event: RenderHandEvent) {
		val player = Minecraft.getInstance().player ?: return
		if (!player.getData(ModAttachmentTypes.SNAIL_SHIELD_STATE).isShelled) return

		event.isCanceled = true
	}

	@SubscribeEvent
	fun preserveFovWhileShelled(event: ComputeFovModifierEvent) {
		val player = event.player
		if (!player.getData(ModAttachmentTypes.SNAIL_SHIELD_STATE).isShelled) return

		val movementSpeedAttribute = player.getAttribute(Attributes.MOVEMENT_SPEED) ?: return
		val shellModifier = movementSpeedAttribute.getModifier(SnailShieldItem.SHELLED_MOVEMENT_SPEED_MODIFIER) ?: return
		val shellSpeedMultiplier = 1.0 + shellModifier.amount
		if (shellSpeedMultiplier == 0.0) return

		val walkingSpeed = player.abilities.walkingSpeed
		if (walkingSpeed == 0f) return

		val movementSpeed = movementSpeedAttribute.value
		val movementFovMultiplier = (movementSpeed / walkingSpeed + 1.0) / 2.0
		if (movementFovMultiplier == 0.0) return

		val unshelledMovementSpeed = movementSpeed / shellSpeedMultiplier
		val unshelledMovementFovMultiplier = (unshelledMovementSpeed / walkingSpeed + 1.0) / 2.0
		val correctedFovModifier = event.fovModifier * unshelledMovementFovMultiplier / movementFovMultiplier
		val fovEffectScale = Minecraft.getInstance().options.fovEffectScale().get().toFloat()

		event.newFovModifier = Mth.lerp(
			fovEffectScale,
			1f,
			correctedFovModifier.toFloat()
		)
	}

	@SubscribeEvent
	fun handleInteractionInput(event: InputEvent.InteractionKeyMappingTriggered) {
		val player = Minecraft.getInstance().player
		if (player != null
			&& player.getData(ModAttachmentTypes.SNAIL_SHIELD_STATE).isShelled
			&& (event.isAttack || event.isUseItem)
		) {
			event.isCanceled = true
			event.setSwingHand(false)
			return
		}

		if (player != null && CritterCageItem.tryClickSnail(player, event.hand)) {
			event.isCanceled = true
			return
		}

		ClientWebLineInteractionHandler.handleInteraction(event)
	}

	@SubscribeEvent
	fun onLoggingOut(event: ClientPlayerNetworkEvent.LoggingOut) {
		ClientEvilSnails.clear()
		ClientWebLines.clear()
	}

	@SubscribeEvent
	fun afterClientTick(event: ClientTickEvent.Post) {
		val player = AaronClientUtil.localPlayer ?: return
		SnailShieldMovedPacket.handleClientMovement(player)
	}

	@SubscribeEvent
	fun registerEntityRenderers(event: EntityRenderersEvent.RegisterRenderers) {
		event.registerBlockEntityRenderer(ModBlockEntityTypes.CRITTER_CAGE.get(), ::CritterCageBlockRenderer)
		event.registerBlockEntityRenderer(ModBlockEntityTypes.HOPPING_SPIDER_NEST.get(), ::HoppingSpiderNestBlockRenderer)
		event.registerBlockEntityRenderer(ModBlockEntityTypes.SPARKBUG_BUSH.get(), ::SparkbugBushBlockRenderer)

		event.registerEntityRenderer(ModEntityTypes.FILTERFEEDER.get(), ::FilterfeederRenderer)
		event.registerEntityRenderer(ModEntityTypes.PUNCHPRAWN.get(), ::PunchprawnRenderer)
		event.registerEntityRenderer(ModEntityTypes.SCOOCHWORM.get(), ::ScoochwormRenderer)
		event.registerEntityRenderer(ModEntityTypes.SCOOCHWORM_PART.get(), ::ScoochwormPartRenderer)
		event.registerEntityRenderer(ModEntityTypes.SPROUTSNAIL.get(), ::SproutsnailRenderer)
	}

	@SubscribeEvent
	fun addEntityRenderLayers(event: EntityRenderersEvent.AddLayers) {
		SnailShieldRenderer.initialize(event.context)

		for (skin in event.skins) {
			val playerRenderer = event.getSkin<PlayerRenderer>(skin) ?: continue
			playerRenderer.addLayer(PunchprawnHatLayer(playerRenderer))
		}
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