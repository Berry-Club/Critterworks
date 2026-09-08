package dev.aaronhowser.mods.critterworks.client.render.entity

import com.mojang.blaze3d.vertex.PoseStack
import dev.aaronhowser.mods.aaron.client.render.AaronRenderUtil
import dev.aaronhowser.mods.aaron.misc.AaronDsls.withPose
import dev.aaronhowser.mods.critterworks.Critterworks
import dev.aaronhowser.mods.critterworks.config.ClientConfig
import dev.aaronhowser.mods.critterworks.entity.ScoochwormEntity
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.RenderLevelStageEvent

@EventBusSubscriber(
	modid = Critterworks.MOD_ID,
	value = [Dist.CLIENT]
)
object ScoochwormPathRenderer {

	@SubscribeEvent
	fun renderPaths(event: RenderLevelStageEvent) {
		if (event.stage != RenderLevelStageEvent.Stage.AFTER_WEATHER) return
		if (!ClientConfig.CONFIG.renderScoochwormPath.get()) return

		val minecraft = Minecraft.getInstance()
		val level = minecraft.level ?: return
		val cameraPosition = event.camera.position
		val poseStack = event.poseStack
		val bufferSource = minecraft.renderBuffers().bufferSource()

		poseStack.withPose {
			poseStack.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z)

			for (entity in level.entitiesForRendering()) {
				if (entity !is ScoochwormEntity) continue
				renderPath(entity, poseStack, bufferSource)
			}
		}
	}

	private fun renderPath(
		entity: ScoochwormEntity,
		poseStack: PoseStack,
		bufferSource: MultiBufferSource
	) {
		val points = entity.pathPoints
		if (points.size < 2) return

		for (index in 0 until points.lastIndex) {
			val first = points[index].position
			val second = points[index + 1].position
			AaronRenderUtil.renderLineThroughWalls(
				poseStack,
				bufferSource,
				first,
				second,
				getPathColor(entity)
			)
		}
	}

	private fun getPathColor(entity: ScoochwormEntity): Int {
		val uuidBits = entity.uuid.mostSignificantBits xor entity.uuid.leastSignificantBits
		val colorBits = (uuidBits xor (uuidBits ushr 32)).toInt()
		return PATH_ALPHA or (colorBits and 0x00FFFFFF)
	}

	private const val PATH_ALPHA = 0xFF000000.toInt()
}