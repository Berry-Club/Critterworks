package dev.aaronhowser.mods.critterworks.client.web

import com.mojang.blaze3d.vertex.PoseStack
import dev.aaronhowser.mods.aaron.client.render.AaronRenderUtil
import dev.aaronhowser.mods.aaron.misc.AaronDsls.withPose
import dev.aaronhowser.mods.critterworks.Critterworks
import net.minecraft.client.Minecraft
import net.minecraft.world.phys.Vec3
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.ClientTickEvent
import net.neoforged.neoforge.client.event.RenderLevelStageEvent

@EventBusSubscriber(modid = Critterworks.MOD_ID, value = [Dist.CLIENT])
object WebPathIndicatorRenderer {

	private var indicatedPositions = emptyList<Vec3>()
	private var ticksLeft = 0

	fun show(positions: List<Vec3>, durationTicks: Int) {
		indicatedPositions = positions
		ticksLeft = durationTicks
	}

	@SubscribeEvent
	fun afterClientTick(event: ClientTickEvent.Post) {
		if (Minecraft.getInstance().isPaused || ticksLeft <= 0) return

		ticksLeft--
		if (ticksLeft == 0) indicatedPositions = emptyList()
	}

	@SubscribeEvent
	fun renderIndicators(event: RenderLevelStageEvent) {
		if (event.stage != RenderLevelStageEvent.Stage.AFTER_WEATHER || ticksLeft <= 0) return

		val cameraPosition = event.camera.position
		val poseStack = event.poseStack
		poseStack.withPose {
			poseStack.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z)
			for (position in indicatedPositions) {
				renderIndicator(poseStack, position)
			}
		}
	}

	private fun renderIndicator(poseStack: PoseStack, position: Vec3) {
		AaronRenderUtil.renderCubeThroughWalls(
			poseStack,
			position.x - INDICATOR_RADIUS,
			position.y - INDICATOR_RADIUS,
			position.z - INDICATOR_RADIUS,
			position.x + INDICATOR_RADIUS,
			position.y + INDICATOR_RADIUS,
			position.z + INDICATOR_RADIUS,
			INDICATOR_COLOR
		)
	}

	private const val INDICATOR_RADIUS = 0.1
	private const val INDICATOR_COLOR = 0x80FF55FF.toInt()
}