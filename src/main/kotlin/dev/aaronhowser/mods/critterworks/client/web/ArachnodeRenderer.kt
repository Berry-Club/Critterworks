package dev.aaronhowser.mods.critterworks.client.web

import com.mojang.blaze3d.vertex.PoseStack
import dev.aaronhowser.mods.aaron.misc.AaronDsls.withPose
import dev.aaronhowser.mods.critterworks.config.ClientConfig
import dev.aaronhowser.mods.critterworks.handler.web.redstone.RedstoneWebLine
import dev.aaronhowser.mods.critterworks.handler.web.redstone.node.Arachnode
import dev.aaronhowser.mods.critterworks.handler.web.redstone.node.ArachnodeMount
import dev.aaronhowser.mods.critterworks.handler.web.redstone.node.DirectionalArachnode
import dev.aaronhowser.mods.critterworks.registry.ModItems
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.LevelRenderer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.entity.ItemRenderer
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.core.BlockPos
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import org.joml.Matrix3f
import org.joml.Quaternionf

object ArachnodeRenderer {

	private val hoppingSpiderStack: ItemStack by lazy { ModItems.HOPPING_SPIDER.get().defaultInstance }
	private val itemRenderer: ItemRenderer by lazy { Minecraft.getInstance().itemRenderer }

	fun renderAll(
		bufferSource: MultiBufferSource.BufferSource,
		level: Level,
		poseStack: PoseStack,
		cameraPosition: Vec3
	) {
		for (arachnode in ClientWebState.getArachnodes()) {
			renderArachnode(arachnode, poseStack, cameraPosition, level, bufferSource)
		}

		bufferSource.endBatch()
	}

	private fun renderArachnode(
		arachnode: Arachnode,
		poseStack: PoseStack,
		cameraPosition: Vec3,
		level: Level,
		bufferSource: MultiBufferSource.BufferSource
	) {
		val outputDirection = arachnode.getDisplayDirection() ?: return
		val mount = arachnode.mount

		val spiderUp = if (mount is ArachnodeMount.Surface) {
			mount.surfaceNormal
		} else {
			getSpiderUp(outputDirection)
		}

		val rotation = getTravelRotation(outputDirection, spiderUp)
		val light = LevelRenderer.getLightColor(level, BlockPos.containing(arachnode.position))

		val hoppingSpiderScale = ClientConfig.CONFIG.hoppingSpiderScale.get().toFloat()

		poseStack.withPose {
			val displayPosition = arachnode.position.add(spiderUp.scale(DISTANCE_FROM_LINE))
			val relativePosition = displayPosition.subtract(cameraPosition)
			poseStack.translate(relativePosition.x, relativePosition.y, relativePosition.z)
			poseStack.mulPose(rotation)
			poseStack.scale(hoppingSpiderScale, hoppingSpiderScale, hoppingSpiderScale)

			if (arachnode is DirectionalArachnode) {
				DirectionalArachnodeRenderer.render(
					poseStack,
					bufferSource,
					light,
					isReceivingSignal(arachnode)
				)
			} else {
				itemRenderer.renderStatic(
					hoppingSpiderStack,
					ItemDisplayContext.GROUND,
					light,
					OverlayTexture.NO_OVERLAY,
					poseStack,
					bufferSource,
					level,
					0
				)
			}
		}
	}

	private fun isReceivingSignal(arachnode: DirectionalArachnode): Boolean {
		for (inputPort in arachnode.inputPorts) {
			for (line in inputPort.lines) {
				if (line is RedstoneWebLine && line.power > 0) return true
			}
		}

		return false
	}

	private fun getTravelRotation(direction: Vec3, spiderUp: Vec3): Quaternionf {
		val backward = direction.scale(-1.0)
		val right = spiderUp.cross(backward).normalize()
		val rotationMatrix = Matrix3f()
			.setColumn(0, right.toVector3f())
			.setColumn(1, spiderUp.toVector3f())
			.setColumn(2, backward.toVector3f())

		return Quaternionf().setFromNormalized(rotationMatrix)
	}

	private fun getSpiderUp(direction: Vec3): Vec3 {
		var spiderUp = UP.subtract(direction.scale(UP.dot(direction)))

		if (spiderUp.lengthSqr() < MIN_DIRECTION_LENGTH_SQUARED) {
			spiderUp = NORTH.subtract(direction.scale(NORTH.dot(direction)))
		}

		return spiderUp.normalize()
	}

	private const val DISTANCE_FROM_LINE = 0.04
	private const val MIN_DIRECTION_LENGTH_SQUARED = 1.0e-6

	private val UP = Vec3(0.0, 1.0, 0.0)
	private val NORTH = Vec3(0.0, 0.0, -1.0)

}