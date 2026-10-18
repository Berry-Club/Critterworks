package dev.aaronhowser.mods.critterworks.menu.fluid_filter

import dev.aaronhowser.mods.aaron.menu.textures.ScreenBackground
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.toComponent
import dev.aaronhowser.mods.critterworks.Critterworks
import dev.aaronhowser.mods.critterworks.datagen.language.ModMenuLang
import dev.aaronhowser.mods.critterworks.menu.CritterworksScreen
import dev.aaronhowser.mods.critterworks.menu.ToggleSpriteButton
import dev.aaronhowser.mods.critterworks.menu.item_filter.ItemFilterScreen
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory

class FluidFilterScreen(
	menu: FluidFilterMenu,
	playerInventory: Inventory,
	title: Component
) : CritterworksScreen<FluidFilterMenu>(menu, playerInventory, title) {

	override val background: ScreenBackground = BACKGROUND
	override val inventoryLabelOffsetY: Int = -6

	override fun baseInit() {
		super.baseInit()

		val buttonY = topPos + 110
		val middleX = leftPos + background.width / 2

		val buttonWidth = 20
		val buttonSpacing = 10

		var buttonX = middleX - buttonSpacing - buttonWidth - buttonSpacing

		val invertButton = ToggleSpriteButton(
			x = buttonX,
			y = buttonY,
			width = 20,
			height = 20,
			font = font,
			sprites = Pair(ItemFilterScreen.INVERT_ON, ItemFilterScreen.INVERT_OFF),
			messages = Pair(
				ModMenuLang.INVERTED_ON.toComponent(),
				ModMenuLang.INVERTED_OFF.toComponent()
			),
			isOnGetter = { menu.isInverted() },
			onPress = {
				buttonClicked(FluidFilterMenu.TOGGLE_INVERTED_BUTTON_ID)
			}
		)

		buttonX += buttonWidth + buttonSpacing

		val useTagsButton = ToggleSpriteButton(
			x = buttonX,
			y = buttonY,
			width = 20,
			height = 20,
			font = font,
			sprites = Pair(ItemFilterScreen.USE_TAGS_ON, ItemFilterScreen.USE_TAGS_OFF),
			messages = Pair(
				ModMenuLang.USE_TAGS_ON.toComponent(),
				ModMenuLang.USE_TAGS_OFF.toComponent()
			),
			isOnGetter = { menu.useTags() },
			onPress = {
				buttonClicked(FluidFilterMenu.TOGGLE_USE_TAGS_BUTTON_ID)
			}
		)

		buttonX += buttonWidth + buttonSpacing

		val ignoreAllComponentsButton = ToggleSpriteButton(
			x = buttonX,
			y = buttonY,
			width = 20,
			height = 20,
			font = font,
			sprites = Pair(ItemFilterScreen.IGNORE_ALL_COMPONENTS_ON, ItemFilterScreen.IGNORE_ALL_COMPONENTS_OFF),
			messages = Pair(
				ModMenuLang.IGNORE_ALL_COMPONENTS_ON.toComponent(),
				ModMenuLang.IGNORE_ALL_COMPONENTS_OFF.toComponent()
			),
			isOnGetter = { menu.ignoreAllComponents() },
			onPress = {
				buttonClicked(FluidFilterMenu.TOGGLE_IGNORE_ALL_COMPONENTS_BUTTON_ID)
			}
		)

		addRenderableWidget(invertButton)
		addRenderableWidget(useTagsButton)
		addRenderableWidget(ignoreAllComponentsButton)
	}

	companion object {
		val BACKGROUND = ScreenBackground(Critterworks.modResource("textures/gui/fluid_filter.png"), 176, 241)
	}
}