package dev.aaronhowser.mods.critterworks.item

import dev.aaronhowser.mods.aaron.misc.AaronExtensions.giveOrDropStack
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isClientSide
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isEntity
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isItem
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isServerSide
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isTrue
import dev.aaronhowser.mods.aaron.misc.AaronUtil
import dev.aaronhowser.mods.critterworks.advancement.ModAdvancements
import dev.aaronhowser.mods.critterworks.config.ServerConfig
import dev.aaronhowser.mods.critterworks.datagen.language.ModMessageLang
import dev.aaronhowser.mods.critterworks.entity.scoochworm.ScoochwormEntity
import dev.aaronhowser.mods.critterworks.handler.snail.ClientEvilSnails
import dev.aaronhowser.mods.critterworks.handler.snail.EvilSnailHandler
import dev.aaronhowser.mods.critterworks.packet.client_to_server.UseCageOnEvilSnailPacket
import dev.aaronhowser.mods.critterworks.registry.ModBlocks
import dev.aaronhowser.mods.critterworks.registry.ModDataComponents
import dev.aaronhowser.mods.critterworks.registry.ModEntityTypes
import dev.aaronhowser.mods.critterworks.registry.ModItems
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.Mth
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.HitResult
import net.neoforged.neoforge.common.Tags
import kotlin.math.cos
import kotlin.math.sin

class CritterCageItem(properties: Properties) : BlockItem(ModBlocks.CRITTER_CAGE.get(), properties) {

	override fun useOn(context: UseOnContext): InteractionResult {
		val stack = context.itemInHand
		val player = context.player
		if (!stack.has(ModDataComponents.ENTITY_DATA) || player?.isSecondaryUseActive.isTrue()) {
			return super.useOn(context)
		}

		val level = context.level
		if (level.isClientSide) return InteractionResult.SUCCESS

		val clickedPosition = context.clickedPos
		val clickedState = level.getBlockState(clickedPosition)
		val spawnPosition = if (clickedState.getCollisionShape(level, clickedPosition).isEmpty) {
			clickedPosition
		} else {
			clickedPosition.relative(context.clickedFace)
		}

		val entity = placeEntity(
			stack,
			level,
			spawnPosition,
			context.horizontalDirection
		) ?: return InteractionResult.FAIL

		if (!level.noCollision(entity)) {
			entity.discard()
			return InteractionResult.FAIL
		}
		if (!level.addFreshEntity(entity)) return InteractionResult.FAIL

		stack.remove(ModDataComponents.ENTITY_DATA)
		return InteractionResult.CONSUME
	}

	override fun interactLivingEntity(
		stack: ItemStack,
		player: Player,
		interactionTarget: LivingEntity,
		usedHand: InteractionHand
	): InteractionResult {
		if (stack.has(ModDataComponents.ENTITY_DATA)) return InteractionResult.PASS
		if (!canCapture(interactionTarget)) return InteractionResult.PASS

		if (player.isServerSide) {
			val filledCage = stack.copyWithCount(1)
			filledCage.set(ModDataComponents.ENTITY_DATA, createEntityData(interactionTarget))

			if (!player.hasInfiniteMaterials() && stack.count == 1) {
				player.setItemInHand(usedHand, filledCage)
			} else {
				stack.consume(1, player)
				player.giveOrDropStack(filledCage)
			}

			interactionTarget.discard()

			ModAdvancements.trigger(player, ModAdvancements.USE_CRITTER_CAGE)
		}

		return InteractionResult.sidedSuccess(player.isClientSide)
	}

	companion object {
		private const val ENTITY_TYPE_TAG = "id"

		fun canCapture(entity: LivingEntity): Boolean {
			return entity.type.canSerialize() && !entity.type.isEntity(Tags.EntityTypes.CAPTURING_NOT_SUPPORTED)
		}

		fun createEntityData(entity: LivingEntity): CustomData {
			val entityTag = CompoundTag()
			entity.saveAsPassenger(entityTag)
			AaronUtil.cleanEntityNbt(entityTag, stripUniqueness = true)
			entityTag.putString(ENTITY_TYPE_TAG, EntityType.getKey(entity.type).toString())

			if (entity is ScoochwormEntity) {
				val removeTags = listOf(
					ScoochwormEntity.PATH_TAG,
					ScoochwormEntity.TRYING_TO_MOVE_TAG,
					ScoochwormEntity.SUPPORT_DIRECTION_TAG,
					ScoochwormEntity.SUPPORT_POSITION_TAG,
					ScoochwormEntity.MOVEMENT_DIRECTION_TAG
				)

				for (tagName in removeTags) {
					entityTag.remove(tagName)
				}
			}

			return CustomData.of(entityTag)
		}

		fun createEntity(entityData: CustomData, level: Level): LivingEntity? {
			val tag = entityData.copyTag()
			val entityType = EntityType.by(tag).orElse(null)

			return try {
				if (entityType == null) {
					if (tag.contains(ENTITY_TYPE_TAG)) {
						null
					} else {
						createScoochworm(entityData, level)
					}
				} else {
					val entity = entityType.create(level)
					if (entity is LivingEntity) {
						entity.load(tag)
						entity
					} else {
						null
					}
				}
			} catch (_: Exception) {
				null
			}
		}

		fun placeEntity(
			stack: ItemStack,
			level: Level,
			spawnPos: BlockPos,
			facingDirection: Direction
		): LivingEntity? {
			val entityData = stack.get(ModDataComponents.ENTITY_DATA) ?: return null
			val entity = createEntity(entityData, level) ?: return null
			entity.setPos(spawnPos.bottomCenter)

			if (entity !is ScoochwormEntity) {
				entity.yRot = facingDirection.toYRot()
				entity.yRotO = entity.yRot
				return entity
			}

			ScoochwormEntity.finishPlacement(
				entity,
				level,
				spawnPos,
				facingDirection
			)
			return entity
		}

		private fun createScoochworm(entityData: CustomData, level: Level): ScoochwormEntity {
			val scoochworm = ScoochwormEntity(ModEntityTypes.SCOOCHWORM.get(), level)
			scoochworm.load(entityData.copyTag())
			return scoochworm
		}

		fun tryClickSnail(player: Player, hand: InteractionHand): Boolean {
			val cage = player.getItemInHand(hand)
			if (!cage.isItem(ModItems.CRITTER_CAGE) || cage.has(ModDataComponents.ENTITY_DATA)) return false

			if (player is ServerPlayer) {
				return tryClickSnailOnServer(player)
			}

			val snail = ClientEvilSnails.getSnail(player.uuid) ?: return false
			val snailPosition = snail.getPosition(System.nanoTime())
			val snailBounds = AABB.ofSize(snailPosition, 1.0, 1.0, 1.0)
			if (!isLookingAtSnail(player, snailBounds)) return false

			UseCageOnEvilSnailPacket(hand).messageServer()
			return true
		}

		private fun tryClickSnailOnServer(player: ServerPlayer): Boolean {
			val snailHandler = EvilSnailHandler.get(player.serverLevel())
			val snail = snailHandler.getSnail(player) ?: return false
			if (snail.dimension != player.level().dimension()) return false
			if (!isLookingAtSnail(player, snail.boundingBox)) return false

			val restartDistance = ServerConfig.CONFIG.sproutsnailEvilCageRestartDistance.get()
			val angle = player.random.nextDouble() * Mth.TWO_PI.toDouble()
			val restartPosition = player.position()
				.add(
					cos(angle) * restartDistance,
					0.0,
					sin(angle) * restartDistance
				)

			snailHandler.addSnail(player, restartPosition)
			player.sendSystemMessage(Component.translatable(ModMessageLang.EVIL_SNAIL_CAGE_MESSAGE))
			return true
		}

		private fun isLookingAtSnail(player: Player, snailBounds: AABB): Boolean {
			val eyePosition = player.eyePosition
			val lookEnd = eyePosition.add(player.lookAngle.scale(player.entityInteractionRange()))
			val snailHit = snailBounds.clip(eyePosition, lookEnd).orElse(null) ?: return false
			val blockHit = player.level().clip(
				ClipContext(
					eyePosition,
					lookEnd,
					ClipContext.Block.COLLIDER,
					ClipContext.Fluid.NONE,
					player
				)
			)

			if (blockHit.type == HitResult.Type.MISS) return true

			return eyePosition.distanceToSqr(snailHit) <= eyePosition.distanceToSqr(blockHit.location)
		}
	}

}