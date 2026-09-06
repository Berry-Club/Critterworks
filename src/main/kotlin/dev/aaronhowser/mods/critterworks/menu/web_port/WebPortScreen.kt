package dev.aaronhowser.mods.critterworks.menu.web_port

import dev.aaronhowser.mods.aaron.menu.BaseScreen
import dev.aaronhowser.mods.aaron.menu.ScreenWithStrings
import dev.aaronhowser.mods.aaron.menu.textures.ScreenBackground
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.getDyeName
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.toComponent
import dev.aaronhowser.mods.aaron.packet.c2s.ClientChangedMenuString
import dev.aaronhowser.mods.aaron.packet.c2s.ClientClickedMenuButton
import dev.aaronhowser.mods.critterworks.Critterworks
import dev.aaronhowser.mods.critterworks.datagen.language.ModMenuLang
import dev.aaronhowser.mods.critterworks.menu.ColorButton
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory

class WebPortScreen(menu: WebPortMenu, inventory: Inventory, title: Component) :
	BaseScreen<WebPortMenu>(menu, inventory, title), ScreenWithStrings {

	override val background: ScreenBackground = BACKGROUND
	override val inventoryLabelOffsetY: Int = -2

	private lateinit var colorButton: ColorButton
	private lateinit var directionButton: Button
	private lateinit var priorityInput: EditBox

	override fun baseInit() {
		super.baseInit()

		colorButton = ColorButton(
			leftPos + 56,
			topPos + 42,
			20,
			20,
			font,
			{ menu.getColor().textColor },
			{ Component.literal(menu.getColor().getDyeName()) },
		) {
			ClientClickedMenuButton(WebPortMenu.CYCLE_COLOR_BUTTON_ID).messageServer()
		}

		directionButton = Button
			.builder(getDirectionMessage()) {
				ClientClickedMenuButton(WebPortMenu.TOGGLE_DIRECTION_BUTTON_ID).messageServer()
			}.bounds(
				leftPos + 88,
				topPos + 42,
				72,
				20
			)
			.build()

		priorityInput = EditBox(
			font,
			leftPos + 56,
			topPos + 20,
			56,
			18,
			ModMenuLang.WEB_PORT_PRIORITY.toComponent()
		)

		priorityInput.setMaxLength(11)
		priorityInput.setFilter { value -> value.isEmpty() || value == "-" || value.toIntOrNull() != null }
		priorityInput.value = menu.getPriority().toString()
		priorityInput.setResponder(::setPriority)

		addRenderableWidget(colorButton)
		addRenderableWidget(directionButton)
		addRenderableWidget(priorityInput)
	}

	private fun setPriority(value: String) {
		if (value.toIntOrNull() == null) return
		ClientChangedMenuString(WebPortMenu.PRIORITY_STRING_ID, value).messageServer()
	}

	override fun receivedString(stringId: Int, stringReceived: String) {
		if (stringId == WebPortMenu.PRIORITY_STRING_ID) {
			priorityInput.value = stringReceived
		}
	}

	override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
		return if (!priorityInput.keyPressed(keyCode, scanCode, modifiers) && !priorityInput.canConsumeInput()) {
			super.keyPressed(keyCode, scanCode, modifiers)
		} else {
			true
		}
	}

	override fun resize(minecraft: Minecraft, width: Int, height: Int) {
		val currentPriority = priorityInput.value
		super.resize(minecraft, width, height)
		priorityInput.value = currentPriority
	}

	override fun containerTick() {
		super.containerTick()
		directionButton.message = getDirectionMessage()
	}

	override fun renderLabels(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int) {
		super.renderLabels(guiGraphics, mouseX, mouseY)
		guiGraphics.drawString(font, ModMenuLang.WEB_PORT_PRIORITY.toComponent(), 12, 25, 0x404040, false)
		guiGraphics.drawString(font, Component.literal("Color: "), 12, 47, 0x404040, false)
	}

	private fun getDirectionMessage(): Component {
		val key = if (menu.isInput()) ModMenuLang.WEB_PORT_INPUT else ModMenuLang.WEB_PORT_OUTPUT
		return key.toComponent()
	}

	companion object {
		val BACKGROUND = ScreenBackground(Critterworks.modResource("textures/gui/web_port.png"), 176, 166)
	}
}