package dev.aaronhowser.mods.critterworks.menu.web_port

import dev.aaronhowser.mods.aaron.menu.MenuWithButtons
import dev.aaronhowser.mods.aaron.menu.MenuWithInventory
import dev.aaronhowser.mods.aaron.menu.MenuWithStrings
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isItem
import dev.aaronhowser.mods.critterworks.handler.web.WebSavedData
import dev.aaronhowser.mods.critterworks.handler.web.node.WebBlockAnchor
import dev.aaronhowser.mods.critterworks.item.WebPortItem
import dev.aaronhowser.mods.critterworks.item.component.WebPortComponent
import dev.aaronhowser.mods.critterworks.registry.ModItems
import dev.aaronhowser.mods.critterworks.registry.ModMenuTypes
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.DataSlot
import net.minecraft.world.item.DyeColor
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.items.ItemStackHandler
import net.neoforged.neoforge.items.SlotItemHandler
import java.util.*

class WebPortMenu private constructor(
	containerId: Int,
	playerInventory: Inventory,
	private val hand: InteractionHand?,
	private val anchorUuid: UUID?,
	private val clientAnchorStack: ItemStack
) : MenuWithInventory(ModMenuTypes.WEB_PORT.get(), containerId, playerInventory), MenuWithButtons, MenuWithStrings {

	constructor(containerId: Int, playerInventory: Inventory, hand: InteractionHand) :
		this(containerId, playerInventory, hand, null, ItemStack.EMPTY)

	constructor(containerId: Int, playerInventory: Inventory, anchor: WebBlockAnchor) :
		this(containerId, playerInventory, null, anchor.uuid, ItemStack.EMPTY)

	private val filterSlot = object : ItemStackHandler(1) {
		override fun isItemValid(slot: Int, stack: ItemStack): Boolean {
			return stack.isItem(ModItems.ITEM_FILTER)
		}

		override fun onContentsChanged(slot: Int) {
			WebPortItem.setFilter(getWebPortStack(), getStackInSlot(slot))
			syncAnchor()
		}
	}

	init {
		filterSlot.setStackInSlot(0, getComponent().getFilter())
		addDataSlot(object : DataSlot() {
			override fun get(): Int = getComponent().color.ordinal
			override fun set(value: Int) {
				WebPortItem.setColor(getWebPortStack(), DyeColor.entries[value])
			}
		})
		addDataSlot(object : DataSlot() {
			override fun get(): Int = getComponent().transferDirection.ordinal
			override fun set(value: Int) {
				val direction = WebPortComponent.TransferDirection.entries[value]
				WebPortItem.setTransferDirection(getWebPortStack(), direction)
			}
		})
		addSlots(84)
	}

	private fun getWebPortStack(): ItemStack {
		val hand = hand
		if (hand != null) return playerInventory.player.getItemInHand(hand)

		val level = playerInventory.player.level()
		if (level is ServerLevel) {
			return getAnchor(level)?.webPort ?: ItemStack.EMPTY
		}
		return clientAnchorStack
	}

	private fun getAnchor(level: ServerLevel): WebBlockAnchor? {
		val uuid = anchorUuid ?: return null
		return WebSavedData.get(level).getNode(uuid) as? WebBlockAnchor
	}

	private fun getComponent(): WebPortComponent {
		return WebPortItem.getComponent(getWebPortStack())
	}

	private fun syncAnchor() {
		val level = playerInventory.player.level()
		if (level !is ServerLevel) return
		val anchor = getAnchor(level) ?: return
		WebSavedData.get(level).syncAnchor(level, anchor)
	}

	fun getColor(): DyeColor = getComponent().color
	fun isInput(): Boolean = getComponent().transferDirection == WebPortComponent.TransferDirection.INPUT
	fun getPriority(): Int = getComponent().priority

	override fun receiveString(stringId: Int, stringReceived: String) {
		if (stringId != PRIORITY_STRING_ID) return
		val priority = stringReceived.toIntOrNull() ?: return
		setPriority(priority)
	}

	fun setPriority(priority: Int) {
		WebPortItem.setPriority(getWebPortStack(), priority)
		syncAnchor()
	}

	override fun addContainerSlots() {
		addSlot(SlotItemHandler(filterSlot, 0, 80, 35))
	}

	override fun stillValid(player: Player): Boolean {
		val hand = hand
		if (hand != null) return player.getItemInHand(hand).isItem(ModItems.WEB_PORT)

		val level = player.level()
		if (level !is ServerLevel) return true
		val anchor = getAnchor(level) ?: return false
		return anchor.hasWebPort && player.distanceToSqr(anchor.position) <= MAX_DISTANCE_SQUARED
	}

	override fun handleButtonPressed(buttonId: Int) {
		val stack = getWebPortStack()
		when (buttonId) {
			CYCLE_COLOR_BUTTON_ID -> {
				val colors = DyeColor.entries
				val nextIndex = (getComponent().color.ordinal + 1) % colors.size
				WebPortItem.setColor(stack, colors[nextIndex])
			}

			TOGGLE_DIRECTION_BUTTON_ID -> {
				val direction = getComponent().transferDirection.next()
				WebPortItem.setTransferDirection(stack, direction)
			}
		}
		syncAnchor()
	}

	companion object {
		const val CYCLE_COLOR_BUTTON_ID = 0
		const val TOGGLE_DIRECTION_BUTTON_ID = 1
		const val PRIORITY_STRING_ID = 0
		private const val MAX_DISTANCE_SQUARED = 64.0

		fun fromNetwork(
			containerId: Int,
			playerInventory: Inventory,
			data: RegistryFriendlyByteBuf
		): WebPortMenu {
			val targetsAnchor = data.readBoolean()
			if (!targetsAnchor) {
				return WebPortMenu(
					containerId,
					playerInventory,
					data.readEnum(InteractionHand::class.java)
				)
			}

			val anchorUuid = data.readUUID()
			val webPortStack = ItemStack.OPTIONAL_STREAM_CODEC.decode(data)
			return WebPortMenu(containerId, playerInventory, null, anchorUuid, webPortStack)
		}
	}
}