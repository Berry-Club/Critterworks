package dev.aaronhowser.mods.critterworks.menu.spider_nest

import dev.aaronhowser.mods.aaron.menu.BaseScreen
import dev.aaronhowser.mods.aaron.menu.textures.ScreenBackground
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.toComponent
import dev.aaronhowser.mods.critterworks.Critterworks
import dev.aaronhowser.mods.critterworks.datagen.language.ModMenuLang
import dev.aaronhowser.mods.critterworks.handler.spider.HoppingSpider
import dev.aaronhowser.mods.critterworks.handler.spider.behavior.transport.HoppingSpiderTransportBehavior
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import java.util.*

class SpiderNestScreen(
	menu: SpiderNestMenu,
	playerInventory: Inventory,
	title: Component
) : BaseScreen<SpiderNestMenu>(menu, playerInventory, title) {

	override val background: ScreenBackground = BACKGROUND
	override val showInventoryLabel: Boolean = false

	override fun renderLabels(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int) {
		super.renderLabels(guiGraphics, mouseX, mouseY)
		val nest = menu.getNest() ?: return
		val gameTime = nest.level?.gameTime ?: 0

		for ((index, spider) in nest.hoppingSpiders.withIndex()) {
			renderSpider(guiGraphics, spider, index, gameTime)
		}
	}

	private fun renderSpider(
		guiGraphics: GuiGraphics,
		spider: HoppingSpider,
		index: Int,
		gameTime: Long
	) {
		val rowY = FIRST_ROW_Y + index * ROW_HEIGHT
		val name = if (spider.customName == null) {
			ModMenuLang.SPIDER_NEST_SPIDER.toComponent(index + 1)
		} else {
			spider.customName.toComponent()
		}
		val position = getPositionText(spider, gameTime)
		val transportBehavior = getJobText(spider)

		guiGraphics.drawString(font, name, TEXT_X, rowY, LABEL_COLOR, false)
		guiGraphics.drawString(font, position, TEXT_X, rowY + LINE_HEIGHT, TEXT_COLOR, false)
		guiGraphics.drawString(font, transportBehavior, TEXT_X, rowY + LINE_HEIGHT * 2, TEXT_COLOR, false)
	}

	private fun getPositionText(spider: HoppingSpider, gameTime: Long): Component {
		val position = spider.getRenderPosition(gameTime, 0f)
			?: return ModMenuLang.SPIDER_NEST_POSITION.toComponent("?", "?", "?")

		return ModMenuLang.SPIDER_NEST_POSITION.toComponent(
			formatCoordinate(position.x),
			formatCoordinate(position.y),
			formatCoordinate(position.z)
		)
	}

	private fun formatCoordinate(coordinate: Double): String {
		return String.format(Locale.ROOT, "%.1f", coordinate)
	}

	private fun getJobText(spider: HoppingSpider): Component {
		val transportBehavior = spider.transportBehavior
		if (transportBehavior == null) {
			if (spider.activeBehavior != null) {
				return ModMenuLang.SPIDER_NEST_WANDERING.toComponent()
			}

			return ModMenuLang.SPIDER_NEST_IDLE.toComponent()
		}

		return when (transportBehavior.phase) {
			HoppingSpiderTransportBehavior.Phase.TO_SOURCE -> ModMenuLang.SPIDER_NEST_COLLECTING.toComponent(
				transportBehavior.transferAmount
			)

			HoppingSpiderTransportBehavior.Phase.TO_DESTINATION -> getDeliveryText(spider, transportBehavior)

			HoppingSpiderTransportBehavior.Phase.RETURNING_ITEM -> ModMenuLang.SPIDER_NEST_RETURNING_ITEM.toComponent(
				getFailureText(transportBehavior.failureReason)
			)

			HoppingSpiderTransportBehavior.Phase.RETURNING,
			HoppingSpiderTransportBehavior.Phase.RETURNING_FROM_SOURCE ->
				ModMenuLang.SPIDER_NEST_RETURNING.toComponent()
		}
	}

	private fun getDeliveryText(spider: HoppingSpider, transportBehavior: HoppingSpiderTransportBehavior): Component {
		val failureReason = transportBehavior.failureReason

		if (failureReason != null) {
			return ModMenuLang.SPIDER_NEST_WAITING.toComponent(
				getFailureText(failureReason)
			)
		}

		return ModMenuLang.SPIDER_NEST_DELIVERING.toComponent(
			spider.carriedStack.hoverName
		)
	}

	private fun getFailureText(reason: HoppingSpiderTransportBehavior.FailureReason?): Component {
		if (reason == null) return Component.empty()

		val name = reason.name.lowercase(Locale.ROOT)

		return "${ModMenuLang.SPIDER_NEST_FAILURE}.$name".toComponent()
	}

	companion object {
		private const val TEXT_X = 8
		private const val FIRST_ROW_Y = 22
		private const val ROW_HEIGHT = 48
		private const val LINE_HEIGHT = 11
		private const val LABEL_COLOR = 0x404040
		private const val TEXT_COLOR = 0x606060

		val BACKGROUND = ScreenBackground(Critterworks.modResource("textures/gui/spider_nest.png"), 176, 241)
	}
}