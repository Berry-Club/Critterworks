package dev.aaronhowser.mods.critterworks.menu

import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.network.chat.Component
import java.util.function.Supplier

class FilterEntryButton(
	x: Int,
	y: Int,
	width: Int,
	height: Int,
	private val messagesGetter: Supplier<List<Component>>,
	private val colorGetter: Supplier<Int>,
	private val font: Font,
	onPress: OnPress
) : Button(
	x,
	y,
	width,
	height,
	messagesGetter.get().firstOrNull() ?: Component.empty(),
	onPress,
	DEFAULT_NARRATION
) {

	override fun renderWidget(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
		val color = colorGetter.get()
		val red = (color shr 16 and 255) / 255.0f
		val green = (color shr 8 and 255) / 255.0f
		val blue = (color and 255) / 255.0f

		guiGraphics.setColor(red, green, blue, alpha)
		RenderSystem.enableBlend()
		RenderSystem.enableDepthTest()
		guiGraphics.blitSprite(SPRITES[active, isHovered], x, y, width, height)
		guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f)

		if (isMouseOver(mouseX.toDouble(), mouseY.toDouble())) {
			guiGraphics.renderComponentTooltip(font, messagesGetter.get(), mouseX, mouseY)
		}
	}
}