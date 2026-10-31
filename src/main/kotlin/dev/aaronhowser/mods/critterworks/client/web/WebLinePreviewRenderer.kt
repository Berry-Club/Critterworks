package dev.aaronhowser.mods.critterworks.client.web

import com.mojang.blaze3d.vertex.PoseStack
import dev.aaronhowser.mods.aaron.client.render.AaronRenderUtil
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isItem
import dev.aaronhowser.mods.critterworks.handler.web.common.node.WebBlockAnchor
import dev.aaronhowser.mods.critterworks.handler.web.common.node.WebNode
import dev.aaronhowser.mods.critterworks.handler.web.item.ItemWebInteractionHandler
import dev.aaronhowser.mods.critterworks.item.WebstoneWeaverItem
import dev.aaronhowser.mods.critterworks.registry.ModDataComponents
import dev.aaronhowser.mods.critterworks.registry.ModItems
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3

object WebLinePreviewRenderer {

	fun render(
		minecraft: Minecraft,
		bufferSource: MultiBufferSource.BufferSource,
		level: Level,
		poseStack: PoseStack,
		eyePosition: Vec3,
		viewVector: Vec3
	) {
		renderPlacement(minecraft, bufferSource, level, poseStack, eyePosition, viewVector)
		renderHoveredAnchor(minecraft, poseStack, eyePosition, viewVector)
	}

	private fun renderPlacement(
		minecraft: Minecraft,
		bufferSource: MultiBufferSource.BufferSource,
		level: Level,
		poseStack: PoseStack,
		eyePosition: Vec3,
		viewVector: Vec3
	) {
		val player = minecraft.player ?: return
		val itemStack = getHeldWebTool(player.mainHandItem, player.offhandItem) ?: return

		val firstNode = itemStack.get(ModDataComponents.WEB_NODE)?.node ?: return
		val secondNode = getTargetedNode(minecraft, eyePosition, viewVector) ?: return

		val isValid = if (itemStack.isItem(ModItems.WEBSTONE_WEAVER)) {
			val webstoneWeaver = itemStack.item as? WebstoneWeaverItem ?: return
			webstoneWeaver.canCreateLine(level, player, firstNode, secondNode)
		} else {
			ItemWebInteractionHandler.canCreateLine(
				level,
				player,
				itemStack,
				firstNode,
				secondNode
			)
		}

		val color = if (isValid) VALID_PREVIEW_COLOR else INVALID_PREVIEW_COLOR

		WebLineGeometryRenderer.render(
			bufferSource,
			level,
			poseStack,
			firstNode.position,
			secondNode.position,
			eyePosition,
			color
		)
	}

	private fun renderHoveredAnchor(
		minecraft: Minecraft,
		poseStack: PoseStack,
		eyePosition: Vec3,
		viewVector: Vec3
	) {
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

	private fun getHeldWebTool(mainHandItem: ItemStack, offhandItem: ItemStack): ItemStack? {
		if (isWebTool(mainHandItem)) return mainHandItem
		if (isWebTool(offhandItem)) return offhandItem

		return null
	}

	private fun isWebTool(itemStack: ItemStack): Boolean {
		return itemStack.isItem(ModItems.ARTIFICIAL_SPINNERETS) || itemStack.isItem(ModItems.WEBSTONE_WEAVER)
	}

	/** Uses the web target only when it is in front of the normal target. */
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

		return WebBlockAnchor.create(
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