package dev.aaronhowser.mods.critterworks.menu.fluid_filter

import dev.aaronhowser.mods.aaron.menu.components.MultiStageSpriteButton
import dev.aaronhowser.mods.aaron.menu.textures.ScreenBackground
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.toComponent
import dev.aaronhowser.mods.critterworks.Critterworks
import dev.aaronhowser.mods.critterworks.datagen.language.ModMenuLang
import dev.aaronhowser.mods.critterworks.item.component.FluidFilterEntry
import dev.aaronhowser.mods.critterworks.menu.CritterworksScreen
import dev.aaronhowser.mods.critterworks.menu.FilterEntryButton
import dev.aaronhowser.mods.critterworks.menu.item_filter.ItemFilterScreen
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.components.Button
import net.minecraft.network.chat.Component
import net.minecraft.util.Mth
import net.minecraft.world.entity.player.Inventory
import net.neoforged.neoforge.fluids.FluidStack
import net.neoforged.neoforge.fluids.FluidUtil

class FluidFilterScreen(
	menu: FluidFilterMenu,
	playerInventory: Inventory,
	title: Component
) : CritterworksScreen<FluidFilterMenu>(menu, playerInventory, title) {

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
			.addStage(ModMenuLang.WHITELIST.toComponent(), ItemFilterScreen.WHITELIST)
			.addStage(ModMenuLang.BLACKLIST.toComponent(), ItemFilterScreen.BLACKLIST)
			.currentStageGetter { if (menu.isBlacklist()) 1 else 0 }
			.onPress { buttonClicked(FluidFilterMenu.TOGGLE_BLACKLIST_BUTTON_ID) }
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
			onPress = { buttonClicked(FluidFilterMenu.getTypeButtonId(index)) }
		)
		button.visible = menu.getEntries().getOrNull(index) !is FluidFilterEntry.Empty

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
			onPress = { buttonClicked(FluidFilterMenu.getOptionButtonId(index)) }
		)
		button.visible = menu.getEntries().getOrNull(index) !is FluidFilterEntry.Empty

		optionButtons.add(button)
		addRenderableWidget(button)
	}

	private fun getTypeMessages(index: Int): List<Component> {
		val entry = menu.getEntries().getOrNull(index)
		val isFluid = entry is FluidFilterEntry.SpecificFluid

		return listOf(
			ModMenuLang.FILTER_FLUID.toComponent().withStyle(if (isFluid) ChatFormatting.GRAY else ChatFormatting.DARK_GRAY),
			ModMenuLang.FILTER_FLUID_TAG.toComponent().withStyle(if (isFluid) ChatFormatting.DARK_GRAY else ChatFormatting.GRAY)
		)
	}

	private fun getTypeColor(index: Int): Int {
		return if (menu.getEntries().getOrNull(index) is FluidFilterEntry.SpecificFluid) {
			0xFF5969FF.toInt()
		} else {
			0xFF00B7A2.toInt()
		}
	}

	private fun getOptionMessages(index: Int): List<Component> {
		return when (val entry = menu.getEntries().getOrNull(index)) {
			is FluidFilterEntry.SpecificFluid -> listOf(
				ModMenuLang.FILTER_IGNORE_COMPONENTS.toComponent().withStyle(
					if (entry.requireSameComponents) ChatFormatting.DARK_GRAY else ChatFormatting.GRAY
				),
				ModMenuLang.FILTER_REQUIRE_COMPONENTS.toComponent().withStyle(
					if (entry.requireSameComponents) ChatFormatting.GRAY else ChatFormatting.DARK_GRAY
				)
			)
			is FluidFilterEntry.Tag -> getTagMessages(entry)
			else -> listOf(Component.empty())
		}
	}

	private fun getTagMessages(entry: FluidFilterEntry.Tag): List<Component> {
		val messages = mutableListOf<Component>()

		for (tag in getFluidTags(entry)) {
			val formatting = if (tag == entry.tagKey) ChatFormatting.GRAY else ChatFormatting.DARK_GRAY
			messages.add(Component.literal(tag.location().toString()).withStyle(formatting))
		}

		return messages
	}

	private fun getOptionColor(index: Int): Int {
		return when (val entry = menu.getEntries().getOrNull(index)) {
			is FluidFilterEntry.SpecificFluid -> {
				if (entry.requireSameComponents) 0xFF37C63C.toInt() else 0xFFFF5623.toInt()
			}
			is FluidFilterEntry.Tag -> getTagColor(entry)
			else -> 0xFFFFFFFF.toInt()
		}
	}

	private fun getTagColor(entry: FluidFilterEntry.Tag): Int {
		val tags = getFluidTags(entry)
		if (tags.isEmpty()) return 0xFFFFFFFF.toInt()

		val selectedIndex = tags.indexOf(entry.tagKey).coerceAtLeast(0)
		return Mth.hsvToArgb(selectedIndex.toFloat() / tags.size.toFloat(), 1.0f, 1.0f, 0xFF)
	}

	private fun getFluidTags(entry: FluidFilterEntry.Tag) =
		FluidUtil.getFluidContained(entry.backupStack)
			.orElse(FluidStack.EMPTY)
			.tags
			.toList()

	override fun containerTick() {
		super.containerTick()

		for (index in 0 until 9) {
			val visible = menu.getEntries().getOrNull(index) !is FluidFilterEntry.Empty
			typeButtons[index].visible = visible
			optionButtons[index].visible = visible
		}
	}

	companion object {
		val BACKGROUND = ScreenBackground(Critterworks.modResource("textures/gui/fluid_filter.png"), 176, 159)
	}
}