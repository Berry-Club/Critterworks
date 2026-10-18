package dev.aaronhowser.mods.critterworks.client.item.tardigrade

import dev.aaronhowser.mods.aaron.misc.AaronDsls.withPose
import dev.aaronhowser.mods.critterworks.registry.ModDataComponents
import dev.aaronhowser.mods.critterworks.registry.ModItems
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.client.IItemDecorator

object TardigradeItemDecorator : IItemDecorator {

	override fun render(
		guiGraphics: GuiGraphics,
		font: Font,
		stack: ItemStack,
		xOffset: Int,
		yOffset: Int
	): Boolean {
		if (!stack.has(ModDataComponents.HAS_TARDIGRADE)) return false

		val poseStack = guiGraphics.pose()
		poseStack.withPose {
			poseStack.translate(xOffset + OFFSET, yOffset + OFFSET, Z_OFFSET)
			poseStack.scale(SCALE, SCALE, SCALE)
			guiGraphics.renderItem(ModItems.APPLY_TARDIGRADE.toStack(), 0, 0)
		}

		return false
	}

	private const val SCALE = 1f / 2f
	private const val OFFSET = 9f
	private const val Z_OFFSET = 200f

}