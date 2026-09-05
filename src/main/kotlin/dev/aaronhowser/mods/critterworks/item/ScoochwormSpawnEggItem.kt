package dev.aaronhowser.mods.critterworks.item

import dev.aaronhowser.mods.critterworks.entity.ScoochwormEntity
import dev.aaronhowser.mods.critterworks.registry.ModEntityTypes
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MobSpawnType
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.Spawner
import net.minecraft.world.level.gameevent.GameEvent
import net.neoforged.neoforge.common.DeferredSpawnEggItem
import net.neoforged.neoforge.registries.DeferredHolder

class ScoochwormSpawnEggItem(properties: Properties) : DeferredSpawnEggItem(
	ModEntityTypes.SCOOCHWORM,
	0x95E4ED, 0x6DCF72,
	properties
) {

	override fun useOn(context: UseOnContext): InteractionResult {
		val level = context.level
		if (level !is ServerLevel) return InteractionResult.SUCCESS

		val itemStack = context.itemInHand
		val clickedPosition = context.clickedPos
		val clickedFace = context.clickedFace
		val clickedState = level.getBlockState(clickedPosition)
		val spawner = level.getBlockEntity(clickedPosition)

		if (spawner is Spawner) {
			spawner.setEntityId(getType(itemStack), level.random)
			level.sendBlockUpdated(clickedPosition, clickedState, clickedState, 3)
			level.gameEvent(context.player, GameEvent.BLOCK_CHANGE, clickedPosition)
			itemStack.shrink(1)
			return InteractionResult.CONSUME
		}

		val spawnPosition = if (clickedState.getCollisionShape(level, clickedPosition).isEmpty) {
			clickedPosition
		} else {
			clickedPosition.relative(clickedFace)
		}

		val spawnedEntity = getType(itemStack).spawn(
			level,
			itemStack,
			context.player,
			spawnPosition,
			MobSpawnType.SPAWN_EGG,
			true,
			clickedPosition != spawnPosition && clickedFace == Direction.UP
		)

		if (spawnedEntity == null) return InteractionResult.CONSUME

		val player = context.player

		if (spawnedEntity is ScoochwormEntity) {
			ScoochwormEntity.finishPlacement(
				spawnedEntity,
				level,
				clickedPosition,
				clickedFace,
				player
			)
		}

		itemStack.consume(1, player)
		level.gameEvent(player, GameEvent.ENTITY_PLACE, clickedPosition)

		return InteractionResult.CONSUME
	}
}