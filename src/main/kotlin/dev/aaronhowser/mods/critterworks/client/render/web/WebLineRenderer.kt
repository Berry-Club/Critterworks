package dev.aaronhowser.mods.critterworks.client.render.web

import dev.aaronhowser.mods.aaron.misc.AaronExtensions.toVec3
import dev.aaronhowser.mods.critterworks.Critterworks
import net.minecraft.client.Minecraft
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.RenderLevelStageEvent

@EventBusSubscriber(
	modid = Critterworks.MOD_ID,
	value = [Dist.CLIENT]
)
object WebLineRenderer {

	@SubscribeEvent
	fun renderWebLines(event: RenderLevelStageEvent) {
		if (event.stage != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return

		val cameraPosition = event.camera.position
		val viewVector = event.camera.lookVector.toVec3()
		val poseStack = event.poseStack
		val minecraft = Minecraft.getInstance()
		val level = minecraft.level ?: return

		WebLineGeometryRenderer.renderAll(minecraft, level, poseStack, cameraPosition)
		WebPortRenderer.renderAll(minecraft, level, poseStack, cameraPosition)
		WebLinePreviewRenderer.render(minecraft, level, poseStack, cameraPosition, viewVector)
	}

}