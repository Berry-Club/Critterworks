package dev.aaronhowser.mods.critterworks.item

import dev.aaronhowser.mods.aaron.misc.AaronExtensions.giveOrDropStack
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isClientSide
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.tell
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.toComponent
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.toGrayComponent
import dev.aaronhowser.mods.critterworks.datagen.language.ModMenuLang
import dev.aaronhowser.mods.critterworks.datagen.language.ModMessageLang
import dev.aaronhowser.mods.critterworks.entity.CatchableEntity
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.AgeableMob
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.TamableAnimal
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag

class CritterCatcherItem(properties: Properties) : Item(properties) {

	override fun interactLivingEntity(
		stack: ItemStack,
		player: Player,
		interactionTarget: LivingEntity,
		usedHand: InteractionHand
	): InteractionResult {
		if (interactionTarget !is CatchableEntity || player.isClientSide) return InteractionResult.PASS

		if (interactionTarget is TamableAnimal && !interactionTarget.isOwnedBy(player)) {
			player.tell(ModMessageLang.NO_PET_THEFT.toComponent())
			return InteractionResult.FAIL
		}

		if (interactionTarget is AgeableMob && interactionTarget.isBaby) {
			player.tell(ModMessageLang.NO_CATCH_BABY.toComponent())
			return InteractionResult.FAIL
		}

		val entityStack = interactionTarget.getCaughtItemStack()

		player.giveOrDropStack(entityStack)
		interactionTarget.discard()

		return InteractionResult.SUCCESS
	}

	override fun appendHoverText(
		stack: ItemStack,
		context: TooltipContext,
		tooltipComponents: MutableList<Component>,
		tooltipFlag: TooltipFlag
	) {
		tooltipComponents += ModMenuLang.CRITTER_CATCHER_TOOLTIP.toGrayComponent()
	}

}