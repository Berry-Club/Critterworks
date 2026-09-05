package dev.aaronhowser.mods.critterworks.client.render.web

import com.mojang.blaze3d.vertex.PoseStack
import dev.aaronhowser.mods.aaron.client.render.AaronRenderUtil
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isItem
import dev.aaronhowser.mods.critterworks.handler.web.WebLineInteractionHandler
import dev.aaronhowser.mods.critterworks.handler.web.line.ClientWebLineInteractionHandler
import dev.aaronhowser.mods.critterworks.handler.web.node.WebNode
import dev.aaronhowser.mods.critterworks.registry.ModDataComponents
import dev.aaronhowser.mods.critterworks.registry.ModItems
import net.minecraft.client.Minecraft
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3

object WebLinePreviewRenderer {

	fun render(poseStack: PoseStack, eyePosition: Vec3, viewVector: Vec3) {
		renderPlacement(poseStack, eyePosition, viewVector)
		renderHoveredAnchor(poseStack, eyePosition, viewVector)
	}

	private fun renderPlacement(poseStack: PoseStack, eyePosition: Vec3, viewVector: Vec3) {
		val minecraft = Minecraft.getInstance()
		val player = minecraft.player ?: return
		val itemStack = getHeldArtificialSpinnerets(player.mainHandItem, player.offhandItem) ?: return

		val firstNode = itemStack.get(ModDataComponents.WEB_NODE)?.node ?: return
		val secondNode = getTargetedNode(minecraft, eyePosition, viewVector) ?: return

		val isValid = WebLineInteractionHandler.canCreateLine(
			player.level(),
			player,
			itemStack,
			firstNode,
			secondNode
		)

		val color = if (isValid) VALID_PREVIEW_COLOR else INVALID_PREVIEW_COLOR

		WebLineGeometryRenderer.render(
			poseStack,
			firstNode.position,
			secondNode.position,
			eyePosition,
			color
		)
	}

	private fun renderHoveredAnchor(poseStack: PoseStack, eyePosition: Vec3, viewVector: Vec3) {
		val minecraft = Minecraft.getInstance()
		val player = minecraft.player ?: return

		val targetedNode = ClientWebLineInteractionHandler.getHoveredAnchor(
			player,
			eyePosition,
			viewVector
		) ?: return

		val position = targetedNode.node.position.subtract(eyePosition)

		AaronRenderUtil.renderCubeThroughWalls(
			poseStack,
			position.x - ANCHOR_RADIUS,
			position.y - ANCHOR_RADIUS,
			position.z - ANCHOR_RADIUS,
			position.x + ANCHOR_RADIUS,
			position.y + ANCHOR_RADIUS,
			position.z + ANCHOR_RADIUS,
			ANCHOR_COLOR
		)
	}

	private fun getHeldArtificialSpinnerets(mainHandItem: ItemStack, offhandItem: ItemStack): ItemStack? {
		if (mainHandItem.isItem(ModItems.ARTIFICIAL_SPINNERETS.get())) return mainHandItem
		if (offhandItem.isItem(ModItems.ARTIFICIAL_SPINNERETS.get())) return offhandItem

		return null
	}

	private fun getTargetedNode(minecraft: Minecraft, eyePosition: Vec3, viewVector: Vec3): WebNode? {
		val player = minecraft.player ?: return null
		val targetedNode = ClientWebLineInteractionHandler.getHoveredAnchor(
			player,
			eyePosition,
			viewVector
		)
		if (targetedNode != null) return targetedNode.node

		val hitResult = minecraft.hitResult
		if (hitResult !is BlockHitResult) return null
		if (hitResult.type != HitResult.Type.BLOCK) return null

		return WebLineInteractionHandler.createBlockAnchor(
			hitResult.blockPos,
			hitResult.direction,
			hitResult.location
		)
	}

	private const val VALID_PREVIEW_COLOR = 0xFFFFFFFF.toInt()
	private const val INVALID_PREVIEW_COLOR = 0xFFFF0000.toInt()
	private const val ANCHOR_RADIUS = 0.05
	private const val ANCHOR_COLOR = 0xA0FFFFFF.toInt()
}