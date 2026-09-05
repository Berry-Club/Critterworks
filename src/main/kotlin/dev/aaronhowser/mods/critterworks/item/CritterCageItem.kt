package dev.aaronhowser.mods.critterworks.item

import dev.aaronhowser.mods.aaron.misc.AaronExtensions.getMinimalTag
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isClientSide
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isServerSide
import dev.aaronhowser.mods.critterworks.entity.ScoochwormEntity
import dev.aaronhowser.mods.critterworks.registry.ModBlocks
import dev.aaronhowser.mods.critterworks.registry.ModDataComponents
import dev.aaronhowser.mods.critterworks.registry.ModEntityTypes
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.ItemUtils
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.level.Level

class CritterCageItem(properties: Properties) : BlockItem(ModBlocks.CRITTER_CAGE.get(), properties) {

	override fun interactLivingEntity(
		stack: ItemStack,
		player: Player,
		interactionTarget: LivingEntity,
		usedHand: InteractionHand
	): InteractionResult {
		if (interactionTarget !is ScoochwormEntity || stack.has(ModDataComponents.ENTITY_DATA)) return InteractionResult.PASS

		if (player.isServerSide) {
			val filledCage = stack.copyWithCount(1)
			filledCage.set(ModDataComponents.ENTITY_DATA, createEntityData(interactionTarget))
			val remainingStack = ItemUtils.createFilledResult(stack, player, filledCage)
			player.setItemInHand(usedHand, remainingStack)

			interactionTarget.discard()
		}

		return InteractionResult.sidedSuccess(player.isClientSide)
	}

	override fun isFoil(stack: ItemStack): Boolean = stack.has(ModDataComponents.ENTITY_DATA)

	companion object {
		fun createEntityData(scoochworm: ScoochwormEntity): CustomData {
			val wormTag = scoochworm.getMinimalTag(stripUniqueness = true)

			val removeTags = listOf(
				"UUID",
				"Pos",
				"Motion",
				"Rotation",
				ScoochwormEntity.PATH_TAG,
				ScoochwormEntity.TRYING_TO_MOVE_TAG,
				ScoochwormEntity.SUPPORT_DIRECTION_TAG,
				ScoochwormEntity.SUPPORT_POSITION_TAG
			)

			for (tagName in removeTags) {
				wormTag.remove(tagName)
			}

			return CustomData.of(wormTag)
		}

		fun placeScoochworm(
			stack: ItemStack,
			level: Level,
			spawnPos: BlockPos,
			facingDirection: Direction
		): ScoochwormEntity? {
			val entityData = stack.get(ModDataComponents.ENTITY_DATA) ?: return null
			val entity = ScoochwormEntity(ModEntityTypes.SCOOCHWORM.get(), level)
			entity.load(entityData.copyTag())
			entity.setPos(spawnPos.bottomCenter)
			ScoochwormEntity.finishPlacement(
				entity,
				level,
				spawnPos,
				facingDirection
			)
			level.addFreshEntity(entity)
			return entity
		}
	}

}