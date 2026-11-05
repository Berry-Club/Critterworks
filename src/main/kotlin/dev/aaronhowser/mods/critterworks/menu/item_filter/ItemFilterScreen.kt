package dev.aaronhowser.mods.critterworks.menu.item_filter

import dev.aaronhowser.mods.aaron.menu.components.MultiStageSpriteButton
import dev.aaronhowser.mods.aaron.menu.textures.ScreenBackground
import dev.aaronhowser.mods.aaron.menu.textures.ScreenSprite
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.toComponent
import dev.aaronhowser.mods.critterworks.Critterworks
import dev.aaronhowser.mods.critterworks.datagen.language.ModMenuLang
import dev.aaronhowser.mods.critterworks.item.component.ItemFilterEntry
import dev.aaronhowser.mods.critterworks.menu.CritterworksScreen
import dev.aaronhowser.mods.critterworks.menu.FilterEntryButton
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.components.Button
import net.minecraft.network.chat.Component
import net.minecraft.util.Mth
import net.minecraft.world.entity.player.Inventory

class ItemFilterScreen(
	menu: ItemFilterMenu,
	playerInventory: Inventory,
	title: Component
) : CritterworksScreen<ItemFilterMenu>(menu, playerInventory, title) {

	override val background: ScreenBackground = BACKGROUND
	override val showTitleLabel: Boolean = false
	override val inventoryLabelOffsetY: Int = -17

	private val typeButtons = mutableListOf<Button>()
	private val optionButtons = mutableListOf<Button>()

	override fun baseInit() {
		super.baseInit()

		addBlacklistButton()

		for (index in 0 until 9) {
			addTypeButton(index)
			addOptionButton(index)
		}
	}

	private fun addBlacklistButton() {
		val button = MultiStageSpriteButton.Builder(font)
			.location(leftPos + imageWidth - 24, topPos + 5)
			.size(16)
			.addStage(ModMenuLang.WHITELIST.toComponent(), WHITELIST)
			.addStage(ModMenuLang.BLACKLIST.toComponent(), BLACKLIST)
			.currentStageGetter { if (menu.isBlacklist()) 1 else 0 }
			.onPress { buttonClicked(ItemFilterMenu.TOGGLE_BLACKLIST_BUTTON_ID) }
			.build()

		addRenderableWidget(button)
	}

	private fun addTypeButton(index: Int) {
		val button = FilterEntryButton(
			x = leftPos + 8 + index * 18,
			y = topPos + 15,
			width = 8,
			height = 8,
			messagesGetter = { getTypeMessages(index) },
			colorGetter = { getTypeColor(index) },
			font = font,
			onPress = { buttonClicked(ItemFilterMenu.getTypeButtonId(index)) }
		)
		button.visible = menu.getEntries().getOrNull(index) !is ItemFilterEntry.Empty

		typeButtons.add(button)
		addRenderableWidget(button)
	}

	private fun addOptionButton(index: Int) {
		val button = FilterEntryButton(
			x = leftPos + 17 + index * 18,
			y = topPos + 15,
			width = 8,
			height = 8,
			messagesGetter = { getOptionMessages(index) },
			colorGetter = { getOptionColor(index) },
			font = font,
			onPress = { buttonClicked(ItemFilterMenu.getOptionButtonId(index)) }
		)
		button.visible = menu.getEntries().getOrNull(index) !is ItemFilterEntry.Empty

		optionButtons.add(button)
		addRenderableWidget(button)
	}

	private fun getTypeMessages(index: Int): List<Component> {
		val entry = menu.getEntries().getOrNull(index)
		val isItem = entry is ItemFilterEntry.SpecificItem

		return listOf(
			ModMenuLang.FILTER_ITEM.toComponent().withStyle(if (isItem) ChatFormatting.GRAY else ChatFormatting.DARK_GRAY),
			ModMenuLang.FILTER_ITEM_TAG.toComponent().withStyle(if (isItem) ChatFormatting.DARK_GRAY else ChatFormatting.GRAY)
		)
	}

	private fun getTypeColor(index: Int): Int {
		return if (menu.getEntries().getOrNull(index) is ItemFilterEntry.SpecificItem) {
			0xFF5969FF.toInt()
		} else {
			0xFF00B7A2.toInt()
		}
	}

	private fun getOptionMessages(index: Int): List<Component> {
		return when (val entry = menu.getEntries().getOrNull(index)) {
			is ItemFilterEntry.SpecificItem -> componentMatchMessages(entry.componentMatch)
			is ItemFilterEntry.Tag -> getTagMessages(entry)
			else -> listOf(Component.empty())
		}
	}

	private fun getTagMessages(entry: ItemFilterEntry.Tag): List<Component> {
		val messages = mutableListOf<Component>()

		for (tag in entry.backupStack.tags.toList()) {
			val formatting = if (tag == entry.tagKey) ChatFormatting.GRAY else ChatFormatting.DARK_GRAY
			messages.add(Component.literal(tag.location().toString()).withStyle(formatting))
		}

		return messages
	}

	private fun componentMatchMessages(selected: ItemFilterEntry.ComponentMatch): List<Component> {
		return listOf(
			componentMatchMessage(ModMenuLang.FILTER_IGNORE_COMPONENTS, ItemFilterEntry.ComponentMatch.IGNORE_ALL, selected),
			componentMatchMessage(ModMenuLang.FILTER_IGNORE_DAMAGE, ItemFilterEntry.ComponentMatch.IGNORE_DAMAGE, selected),
			componentMatchMessage(ModMenuLang.FILTER_REQUIRE_COMPONENTS, ItemFilterEntry.ComponentMatch.EXACT, selected)
		)
	}

	private fun componentMatchMessage(
		translationKey: String,
		value: ItemFilterEntry.ComponentMatch,
		selected: ItemFilterEntry.ComponentMatch
	): Component {
		val formatting = if (value == selected) ChatFormatting.GRAY else ChatFormatting.DARK_GRAY
		return translationKey.toComponent().withStyle(formatting)
	}

	private fun getOptionColor(index: Int): Int {
		return when (val entry = menu.getEntries().getOrNull(index)) {
			is ItemFilterEntry.SpecificItem -> when (entry.componentMatch) {
				ItemFilterEntry.ComponentMatch.IGNORE_ALL -> 0xFFFF5623.toInt()
				ItemFilterEntry.ComponentMatch.IGNORE_DAMAGE -> 0xFFFFC107.toInt()
				ItemFilterEntry.ComponentMatch.EXACT -> 0xFF37C63C.toInt()
			}
			is ItemFilterEntry.Tag -> getTagColor(entry)
			else -> 0xFFFFFFFF.toInt()
		}
	}

	private fun getTagColor(entry: ItemFilterEntry.Tag): Int {
		val tags = entry.backupStack.tags.toList()
		if (tags.isEmpty()) return 0xFFFFFFFF.toInt()

		val selectedIndex = tags.indexOf(entry.tagKey).coerceAtLeast(0)
		return Mth.hsvToArgb(selectedIndex.toFloat() / tags.size.toFloat(), 1.0f, 1.0f, 0xFF)
	}

	override fun containerTick() {
		super.containerTick()

		for (index in 0 until 9) {
			val visible = menu.getEntries().getOrNull(index) !is ItemFilterEntry.Empty
			typeButtons[index].visible = visible
			optionButtons[index].visible = visible
		}
	}

	companion object {
		val BACKGROUND = ScreenBackground(Critterworks.modResource("textures/gui/item_filter.png"), 176, 159)
		val WHITELIST = ScreenSprite(Critterworks.modResource("filter/whitelist"), 16, 16)
		val BLACKLIST = ScreenSprite(Critterworks.modResource("filter/blacklist"), 16, 16)
	}
}