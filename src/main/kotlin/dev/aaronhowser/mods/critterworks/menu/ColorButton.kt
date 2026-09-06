package dev.aaronhowser.mods.critterworks.menu

import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.network.chat.Component
import java.util.function.IntSupplier

class ColorButton(
	x: Int,
	y: Int,
	width: Int,
	height: Int,
	private val font: Font,
	private val colorGetter: IntSupplier,
	private val tooltipGetter: () -> Component,
	onPress: OnPress
) : Button(x, y, width, height, Component.empty(), onPress, DEFAULT_NARRATION) {

	override fun renderWidget(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
		val color = colorGetter.asInt

		guiGraphics.fill(x, y, x + width, y + height, 0xFF000000.toInt())
		guiGraphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, color or ((alpha * 255.0f).toInt() shl 24))

		if (isHovered) {
			guiGraphics.renderTooltip(font, tooltipGetter(), mouseX, mouseY)
		}
	}

	override fun renderString(guiGraphics: GuiGraphics, font: Font, color: Int) {}
}