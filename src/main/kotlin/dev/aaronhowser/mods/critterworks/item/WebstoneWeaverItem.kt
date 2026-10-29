package dev.aaronhowser.mods.critterworks.item

import dev.aaronhowser.mods.aaron.misc.AaronExtensions.status
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.toComponent
import dev.aaronhowser.mods.critterworks.datagen.language.ModMessageLang
import dev.aaronhowser.mods.critterworks.handler.web.common.node.WebBlockAnchor
import dev.aaronhowser.mods.critterworks.handler.web.common.node.WebLineAnchor
import dev.aaronhowser.mods.critterworks.handler.web.common.node.WebNode
import dev.aaronhowser.mods.critterworks.handler.web.redstone.RedstoneWebHandler
import dev.aaronhowser.mods.critterworks.handler.web.redstone.RedstoneWebLine
import dev.aaronhowser.mods.critterworks.item.component.WebNodeDataComponent
import dev.aaronhowser.mods.critterworks.registry.ModDataComponents
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.phys.HitResult
import java.util.*
import java.util.function.Supplier

class WebstoneWeaverItem(properties: Properties) : Item(properties) {

	override fun useOn(context: UseOnContext): InteractionResult {
		val level = context.level
		if (level !is ServerLevel) return InteractionResult.SUCCESS

		val player = context.player ?: return InteractionResult.PASS
		val selectedAnchor = WebBlockAnchor.create(
			context.clickedPos,
			context.clickedFace,
			context.clickLocation
		)

		handleNodeSelection(level, player, context.itemInHand, selectedAnchor)
		return InteractionResult.CONSUME
	}

	override fun use(
		level: Level,
		player: Player,
		usedHand: InteractionHand
	): InteractionResultHolder<ItemStack> {
		val usedStack = player.getItemInHand(usedHand)
		if (player.isSecondaryUseActive && usedStack.has(ModDataComponents.WEB_NODE)) {
			usedStack.remove(ModDataComponents.WEB_NODE)
			return InteractionResultHolder.sidedSuccess(usedStack, level.isClientSide)
		}

		return super.use(level, player, usedHand)
	}

	override fun isFoil(stack: ItemStack): Boolean {
		return stack.has(ModDataComponents.WEB_NODE)
	}

	fun handleNodeSelection(
		level: ServerLevel,
		player: Player,
		itemStack: ItemStack,
		selectedNode: WebNode
	) {
		val firstNode = itemStack.get(ModDataComponents.WEB_NODE)?.node
		if (firstNode == null) {
			itemStack.set(ModDataComponents.WEB_NODE, WebNodeDataComponent(selectedNode))
			player.status(ModMessageLang.WEBSTONE_FIRST_NODE_MESSAGE.toComponent())
			return
		}

		val savedData = RedstoneWebHandler.get(level)
		if (firstNode is WebLineAnchor && savedData.getLine(firstNode.parentLineUuid) == null) {
			itemStack.remove(ModDataComponents.WEB_NODE)
			return
		}

		val canonicalFirstNode = savedData.getCanonicalNode(firstNode)
		val canonicalSelectedNode = savedData.getCanonicalNode(selectedNode)
		val invalidMessage = getInvalidMessage(level, player, canonicalFirstNode, canonicalSelectedNode)
		if (invalidMessage != null) {
			player.status(invalidMessage.toComponent())
			return
		}

		val line = RedstoneWebLine(UUID.randomUUID(), canonicalFirstNode, canonicalSelectedNode)
		savedData.addLine(level, line)
		itemStack.remove(ModDataComponents.WEB_NODE)
		player.status(ModMessageLang.WEBSTONE_LINE_CREATED_MESSAGE.toComponent())
	}

	private fun getInvalidMessage(
		level: Level,
		player: Player,
		firstNode: WebNode,
		secondNode: WebNode
	): String? {
		if (firstNode.uuid == secondNode.uuid) {
			return ModMessageLang.SAME_LINE_MESSAGE
		}

		if (firstNode is WebLineAnchor
			&& secondNode is WebLineAnchor
			&& firstNode.parentLineUuid == secondNode.parentLineUuid
		) {
			return ModMessageLang.SAME_LINE_MESSAGE
		}

		if (firstNode is WebBlockAnchor
			&& secondNode is WebBlockAnchor
			&& firstNode.face == secondNode.face
		) {
			return ModMessageLang.WEBSTONE_SAME_DIRECTION_MESSAGE
		}

		if (firstNode.position.distanceToSqr(secondNode.position) >= MAX_LENGTH * MAX_LENGTH) {
			return ModMessageLang.WEBSTONE_TOO_LONG_MESSAGE
		}

		val clipContext = ClipContext(
			firstNode.position,
			secondNode.position,
			ClipContext.Block.COLLIDER,
			ClipContext.Fluid.NONE,
			player
		)
		if (level.clip(clipContext).type != HitResult.Type.MISS) {
			return ModMessageLang.WEBSTONE_OBSTRUCTED_MESSAGE
		}

		return null
	}

	fun canCreateLine(
		level: Level,
		player: Player,
		firstNode: WebNode,
		secondNode: WebNode
	): Boolean {
		return getInvalidMessage(level, player, firstNode, secondNode) == null
	}

	companion object {
		private const val MAX_LENGTH = 10.0

		val DEFAULT_PROPERTIES: Supplier<Properties> = Supplier {
			Properties().stacksTo(1)
		}
	}
}