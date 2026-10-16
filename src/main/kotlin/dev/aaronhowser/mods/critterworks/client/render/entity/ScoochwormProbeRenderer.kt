package dev.aaronhowser.mods.critterworks.client.render.entity

import dev.aaronhowser.mods.aaron.client.render.AaronRenderUtil
import dev.aaronhowser.mods.aaron.misc.AaronDsls.withPose
import dev.aaronhowser.mods.critterworks.Critterworks
import dev.aaronhowser.mods.critterworks.config.ClientConfig
import dev.aaronhowser.mods.critterworks.entity.scoochworm.ScoochwormEntity
import net.minecraft.client.Minecraft
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.RenderLevelStageEvent

@EventBusSubscriber(
	modid = Critterworks.MOD_ID,
	value = [Dist.CLIENT]
)
object ScoochwormProbeRenderer {

	@SubscribeEvent
	fun renderProbePositions(event: RenderLevelStageEvent) {
		if (event.stage != RenderLevelStageEvent.Stage.AFTER_WEATHER) return
		if (!ClientConfig.CONFIG.renderScoochwormAttachmentProbe.get()) return

		val level = Minecraft.getInstance().level ?: return
		val cameraPosition = event.camera.position
		val poseStack = event.poseStack

		poseStack.withPose {
			poseStack.translate(
				-cameraPosition.x,
				-cameraPosition.y,
				-cameraPosition.z
			)

			for (entity in level.entitiesForRendering()) {
				if (entity !is ScoochwormEntity) continue

				val probePosition = entity.supportPosition ?: continue
				AaronRenderUtil.renderCubeThroughWalls(
					poseStack,
					probePosition.x + CUBE_INSET,
					probePosition.y + CUBE_INSET,
					probePosition.z + CUBE_INSET,
					probePosition.x + 1.0 - CUBE_INSET,
					probePosition.y + 1.0 - CUBE_INSET,
					probePosition.z + 1.0 - CUBE_INSET,
					PROBE_COLOR
				)
			}
		}
	}

	private const val CUBE_INSET = 0.01
	private const val PROBE_COLOR = 0x60FF0000
}