package dev.aaronhowser.mods.critterworks.entity

import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isBlock
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isClientSide
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isItem
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isServerSide
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.nextRange
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.toVec3
import dev.aaronhowser.mods.critterworks.advancement.ModAdvancements
import dev.aaronhowser.mods.critterworks.block.base.ScoochwormTravelBlock
import dev.aaronhowser.mods.critterworks.datagen.tag.ModBlockTagsProvider
import dev.aaronhowser.mods.critterworks.entity.control.ScoochwormStemMoveControl
import dev.aaronhowser.mods.critterworks.entity.data.*
import dev.aaronhowser.mods.critterworks.entity.goal.ScoochwormLookAtFoodGoal
import dev.aaronhowser.mods.critterworks.entity.goal.ScoochwormTravelGoal
import dev.aaronhowser.mods.critterworks.entity.goal.ScoochwormWanderGoal
import dev.aaronhowser.mods.critterworks.registry.ModDataComponents
import dev.aaronhowser.mods.critterworks.registry.ModEntityTypes
import dev.aaronhowser.mods.critterworks.registry.ModSoundEvents
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.gameevent.GameEvent
import net.minecraft.world.level.material.PushReaction
import net.minecraft.world.phys.Vec3
import net.neoforged.neoforge.fluids.FluidType
import software.bernie.geckolib.animatable.GeoEntity
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.animatable.instance.SingletonAnimatableInstanceCache
import software.bernie.geckolib.animation.AnimatableManager
import java.util.*

class ScoochwormEntity(
	entityType: EntityType<ScoochwormEntity>,
	level: Level
) : Mob(entityType, level), GeoEntity {

	private val animatableInstanceCache = SingletonAnimatableInstanceCache(this)
	val stemMoveControl = ScoochwormStemMoveControl(this)
	private val movementPath = ScoochwormPath()
	private val bodySegments = ScoochwormBody(this)

	private var footstepPartIndex = HEAD_FOOTSTEP_INDEX
	private var nextFootstepTick = random.nextInt(FOOTSTEP_CYCLE_PAUSE_TICKS + 1)
	private var nextKissTick = 0

	var rememberedMovementDirection: Vec3? = null
	var isTurningAroundCorner = false

	init {
		moveControl = stemMoveControl
	}

	var supportDirection: Direction
		get() = entityData.get(DATA_SUPPORT_DIRECTION)
		set(value) = entityData.set(DATA_SUPPORT_DIRECTION, value)

	var supportPosition: BlockPos?
		get() = entityData.get(DATA_SUPPORT_POSITION).orElse(null)
		private set(value) = entityData.set(DATA_SUPPORT_POSITION, Optional.ofNullable(value))

	var isTryingToMove: Boolean
		get() = entityData.get(DATA_IS_TRYING_TO_MOVE)
		set(value) {
			if (isServerSide) entityData.set(DATA_IS_TRYING_TO_MOVE, value)
		}

	var color: WormColor
		get() = WormColor.fromOrdinal(entityData.get(DATA_COLOR))
		set(value) {
			entityData.set(DATA_COLOR, value.ordinal)
			bodySegments.updateColor(value)
		}

	override fun registerGoals() {
		goalSelector.addGoal(0, ScoochwormTravelGoal(this))
		goalSelector.addGoal(1, ScoochwormWanderGoal(this))
		goalSelector.addGoal(2, ScoochwormLookAtFoodGoal(this))
		goalSelector.addGoal(3, LookAtPlayerGoal(this, Player::class.java, 6f))
		goalSelector.addGoal(4, RandomLookAroundGoal(this))
	}

	override fun aiStep() {
		if (isServerSide && !hasValidSupport()) {
			supportPosition = null
		}

		isNoGravity = supportPosition != null

		val positionBefore = position()

		super.aiStep()
		bodySegments.tick()

		if (isServerSide) {
			eatTouchingItems()
			kissNearbyWorms()
		}

		if (isClientSide || !isTryingToMove) return

		// Record the path that the head has traveled,
		// and then set each segment to be a set distance from the head along that path
		movementPath.record(position(), supportDirection)
		bodySegments.update(movementPath)

		val moved = positionBefore.distanceToSqr(position()) > 0.000001
		if (moved) {
			playNextFootstep()
		}
	}

	private fun eatTouchingItems() {
		val touchingItems = level().getEntitiesOfClass(
			ItemEntity::class.java,
			boundingBox
		)

		for (itemEntity in touchingItems) {
			if (itemEntity.hasPickUpDelay()) continue

			val itemStack = itemEntity.item
			val remainder = bodySegments.insertIntoLockboxes(itemStack)
			if (remainder.count == itemStack.count) continue

			if (remainder.isEmpty) {
				itemEntity.discard()
			} else {
				itemEntity.item = remainder
			}

			playSound(SoundEvents.GENERIC_EAT, 1f, 1f)
			gameEvent(GameEvent.EAT)
		}
	}

	private fun kissNearbyWorms() {
		val nearbyWorms = level().getEntitiesOfClass(
			ScoochwormEntity::class.java,
			boundingBox.inflate(1.0)
		)

		for (other in nearbyWorms) {
			if (other === this) continue
			kiss(other)
		}
	}

	fun hasValidSupport(): Boolean {
		val currentSupportPosition = supportPosition ?: return false

		return supportsScoochwormTravel(this, level(), currentSupportPosition, supportDirection.opposite)
			|| supportsFreeTravel(level(), currentSupportPosition, supportDirection.opposite)
	}

	fun attachToSupport(position: BlockPos, direction: Direction) {
		supportDirection = direction
		supportPosition = position.immutable()
	}

	private fun playNextFootstep() {
		if (tickCount < nextFootstepTick) return

		val footstepEntity = when (footstepPartIndex) {
			HEAD_FOOTSTEP_INDEX -> this
			else -> bodySegments.getBodyPart(footstepPartIndex)
		} ?: return

		footstepEntity.playSound(
			ModSoundEvents.SCOOCHWORM_FOOTSTEP.get(),
			0.35f,
			random.nextRange(0.85f, 1.15f)
		)

		footstepPartIndex++
		if (footstepPartIndex < bodySegments.size) {
			nextFootstepTick = tickCount + FOOTSTEP_INTERVAL_TICKS
			return
		}

		footstepPartIndex = HEAD_FOOTSTEP_INDEX
		nextFootstepTick = tickCount + FOOTSTEP_CYCLE_PAUSE_TICKS
	}

	override fun remove(reason: RemovalReason) {
		super.remove(reason)
		bodySegments.discard()
	}

	override fun dropEquipment() {
		super.dropEquipment()

		bodySegments.dropAllAttachmentItems()
	}

	// Interaction

	override fun mobInteract(player: Player, hand: InteractionHand): InteractionResult {
		val heldStack = player.getItemInHand(hand)
		val growResult = tryGrow(player, heldStack)
		if (growResult != null) return growResult

		val dyeResult = tryDye(player, heldStack)
		if (dyeResult != null) return dyeResult

		if (!player.isSecondaryUseActive) {
			isTryingToMove = !isTryingToMove
			if (!isTryingToMove) deltaMovement = Vec3.ZERO

			return InteractionResult.sidedSuccess(isClientSide)
		}

		return InteractionResult.PASS
	}

	fun interactWithPart(
		player: Player,
		hand: InteractionHand,
		partIndex: Int?
	): InteractionResult {
		val heldStack = player.getItemInHand(hand)

		val growResult = tryGrow(player, heldStack)
		if (growResult != null) return growResult

		val dyeResult = tryDye(player, heldStack)
		if (dyeResult != null) return dyeResult

		if (heldStack.isItem(Items.SHEARS)) {
			if (partIndex == null || !bodySegments.canSplitAt(partIndex)) {
				return InteractionResult.PASS
			}
		}

		if (partIndex == null) return InteractionResult.PASS

		return bodySegments.interact(
			player,
			hand,
			partIndex
		)
	}

	fun bindClientBodyPart(bodyPart: ScoochwormPartEntity): ScoochwormSegment? {
		return bodySegments.bindClientBodyPart(bodyPart)
	}

	private fun tryGrow(player: Player, heldStack: ItemStack): InteractionResult? {
		if (!heldStack.isItem(Items.MELON) || !bodySegments.canGrow) return null

		if (isServerSide) {
			bodySegments.grow()
			heldStack.consume(1, player)

			playSound(SoundEvents.GENERIC_EAT, 1f, 1f)
			gameEvent(GameEvent.EAT, player)
		}

		return InteractionResult.sidedSuccess(isClientSide)
	}

	private fun tryDye(player: Player, heldStack: ItemStack): InteractionResult? {
		if (!heldStack.has(ModDataComponents.WORM_COLOR)) return null

		val newColor = heldStack.getOrDefault(ModDataComponents.WORM_COLOR, WormColor.GREEN)
		if (newColor == color) return InteractionResult.PASS

		if (isServerSide) {
			color = newColor
			heldStack.consume(1, player)

			ModAdvancements.trigger(player, ModAdvancements.DYE_SCOOCHWORM)
		}

		return InteractionResult.sidedSuccess(isClientSide)
	}

	// Collision

	override fun checkFallDamage(
		yMovement: Double,
		onGround: Boolean,
		blockState: BlockState,
		blockPosition: BlockPos
	) {
		if (onGround && supportPosition != null) fallDistance = 0f

		super.checkFallDamage(yMovement, onGround, blockState, blockPosition)
	}

	override fun canCollideWith(entity: Entity): Boolean {
		return when (entity) {
			is ScoochwormEntity -> entity.id != id
			is ScoochwormPartEntity -> entity.parentId != id
			else -> super.canCollideWith(entity)
		}
	}

	override fun isPushable(): Boolean = false
	override fun isInWall(): Boolean = !isTurningAroundCorner && super.isInWall()
	override fun isPushedByFluid(type: FluidType): Boolean = false
	override fun getPistonPushReaction(): PushReaction = PushReaction.IGNORE
	override fun removeWhenFarAway(distanceToClosestPlayer: Double): Boolean = false
	override fun checkDespawn() {}
	override fun knockback(strength: Double, x: Double, z: Double) {}
	override fun push(entity: Entity) {}
	override fun doPush(entity: Entity) {
		if (entity is ItemEntity) return

		super.doPush(entity)
	}

	fun kiss(other: ScoochwormEntity) {
		if (isClientSide) return
		if (tickCount < nextKissTick || other.tickCount < other.nextKissTick) return

		val movementDirection = stemMoveControl.movementDirection ?: direction
		val otherMovementDirection = other.stemMoveControl.movementDirection ?: other.direction
		if (movementDirection.opposite != otherMovementDirection) return
		if (!sharesMovementLane(other, movementDirection.axis)) return

		val movement = movementDirection.normal.toVec3()
		val otherMovement = otherMovementDirection.normal.toVec3()

		val directionToOther = position().vectorTo(other.position()).normalize()
		if (movement.normalize().dot(directionToOther) < HEAD_ON_ALIGNMENT_DOT) return
		if (otherMovement.normalize().dot(directionToOther.reverse()) < HEAD_ON_ALIGNMENT_DOT) return

		nextKissTick = tickCount + KISS_COOLDOWN_TICKS
		other.nextKissTick = other.tickCount + KISS_COOLDOWN_TICKS

		playSound(ModSoundEvents.SCOOCHWORM_KISS.get(), 1f, 1f)

		for (player in level().players()) {
			if (!player.closerThan(this, COLLISION_WITNESS_DISTANCE_SQUARED)) continue
			ModAdvancements.trigger(player, ModAdvancements.WITNESS_HEAD_ON_COLLISION)
		}
	}

	private fun sharesMovementLane(
		other: ScoochwormEntity,
		movementAxis: Direction.Axis
	): Boolean {
		val position = blockPosition()
		val otherPosition = other.blockPosition()

		return when (movementAxis) {
			Direction.Axis.X -> position.y == otherPosition.y && position.z == otherPosition.z
			Direction.Axis.Y -> position.x == otherPosition.x && position.z == otherPosition.z
			Direction.Axis.Z -> position.x == otherPosition.x && position.y == otherPosition.y
		}
	}

	// Entity data

	override fun defineSynchedData(builder: SynchedEntityData.Builder) {
		super.defineSynchedData(builder)
		builder.define(DATA_SUPPORT_DIRECTION, Direction.DOWN)
		builder.define(DATA_SUPPORT_POSITION, Optional.empty())
		builder.define(DATA_IS_TRYING_TO_MOVE, false)
		builder.define(DATA_COLOR, WormColor.GREEN.ordinal)
	}

	override fun readAdditionalSaveData(tag: CompoundTag) {
		super.readAdditionalSaveData(tag)

		movementPath.load(tag.getList(PATH_TAG, CompoundTag.TAG_COMPOUND.toInt()))
		bodySegments.load(tag.getList(SEGMENTS_TAG, CompoundTag.TAG_COMPOUND.toInt()))
		isTryingToMove = tag.getBoolean(TRYING_TO_MOVE_TAG)
		color = WormColor.fromOrdinal(tag.getInt(COLOR_TAG))

		supportDirection = Direction.from3DDataValue(tag.getInt(SUPPORT_DIRECTION_TAG))
		supportPosition = if (tag.contains(SUPPORT_POSITION_TAG)) {
			BlockPos.of(tag.getLong(SUPPORT_POSITION_TAG))
		} else {
			null
		}

		if (!movementPath.isEmpty()) {
			bodySegments.update(movementPath)
		}
	}

	override fun addAdditionalSaveData(tag: CompoundTag) {
		super.addAdditionalSaveData(tag)
		tag.put(PATH_TAG, movementPath.save())
		tag.put(SEGMENTS_TAG, bodySegments.save())
		tag.putBoolean(TRYING_TO_MOVE_TAG, isTryingToMove)
		tag.putInt(COLOR_TAG, color.ordinal)
		tag.putInt(SUPPORT_DIRECTION_TAG, supportDirection.get3DDataValue())

		val currentSupportPosition = supportPosition
		if (currentSupportPosition != null) {
			tag.putLong(SUPPORT_POSITION_TAG, currentSupportPosition.asLong())
		}
	}

	// Animation

	override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar) {
	}

	override fun getAnimatableInstanceCache(): AnimatableInstanceCache = animatableInstanceCache

	companion object {
		private const val HEAD_ON_ALIGNMENT_DOT = 0.8
		private const val KISS_PROXIMITY = 0.3
		private const val KISS_COOLDOWN_TICKS = 20 * 10
		private const val COLLISION_WITNESS_DISTANCE_SQUARED = 16.0 * 16.0
		const val SIZE = 14f / 16f
		const val PART_SPACING = SIZE * 1.2

		private const val HEAD_FOOTSTEP_INDEX = -1
		private const val FOOTSTEP_INTERVAL_TICKS = 3
		private const val FOOTSTEP_CYCLE_PAUSE_TICKS = 40
		private const val SUPPORT_PROBE_DISTANCE = 0.05

		private const val SEGMENTS_TAG = "Segments"
		const val PATH_TAG = "Path"
		const val TRYING_TO_MOVE_TAG = "Moving"
		const val SUPPORT_DIRECTION_TAG = "AttachmentBottom"
		const val SUPPORT_POSITION_TAG = "AttachmentPosition"
		private const val COLOR_TAG = "Color"

		private val DATA_SUPPORT_DIRECTION: EntityDataAccessor<Direction> =
			SynchedEntityData.defineId(
				ScoochwormEntity::class.java,
				EntityDataSerializers.DIRECTION
			)

		private val DATA_SUPPORT_POSITION: EntityDataAccessor<Optional<BlockPos>> =
			SynchedEntityData.defineId(
				ScoochwormEntity::class.java,
				EntityDataSerializers.OPTIONAL_BLOCK_POS
			)

		private val DATA_IS_TRYING_TO_MOVE: EntityDataAccessor<Boolean> =
			SynchedEntityData.defineId(
				ScoochwormEntity::class.java,
				EntityDataSerializers.BOOLEAN
			)

		private val DATA_COLOR: EntityDataAccessor<Int> =
			SynchedEntityData.defineId(
				ScoochwormEntity::class.java,
				EntityDataSerializers.INT
			)

		fun spawnFromSplit(
			source: ScoochwormEntity,
			headPosition: Vec3,
			supportDirection: Direction,
			yaw: Float,
			segments: List<ScoochwormSegment>,
			pathPoints: List<ScoochwormPathPoint>
		): ScoochwormEntity {
			val level = source.level()

			val scoochworm = ScoochwormEntity(ModEntityTypes.SCOOCHWORM.get(), level)

			scoochworm.moveTo(
				headPosition.x,
				headPosition.y,
				headPosition.z,
				yaw,
				source.xRot
			)

			val supportPosition = getSupportBlockPosition(
				headPosition,
				supportDirection
			)

			scoochworm.attachToSupport(supportPosition, supportDirection)
			level.addFreshEntity(scoochworm)

			scoochworm.bodySegments.replaceWith(segments)
			scoochworm.movementPath.setPoints(pathPoints)
			scoochworm.isTryingToMove = source.isTryingToMove
			scoochworm.color = source.color

			return scoochworm
		}

		fun supportsScoochwormTravel(
			scoochworm: ScoochwormEntity,
			level: Level,
			position: BlockPos,
			attachmentFace: Direction
		): Boolean {
			val blockState = level.getBlockState(position)
			if (!blockState.isBlock(ModBlockTagsProvider.SUPPORTS_SCOOCHWORM_TRAVEL)) return false

			val block = blockState.block
			return if (block is ScoochwormTravelBlock) {
				block.canAttachToBlock(
					blockState,
					scoochworm,
					level,
					position,
					attachmentFace.opposite,
					attachmentFace
				)
			} else {
				true
			}
		}

		fun supportsFreeTravel(
			level: Level,
			position: BlockPos,
			attachmentFace: Direction
		): Boolean {
			val blockState = level.getBlockState(position)
			if (blockState.isBlock(ModBlockTagsProvider.SUPPORTS_SCOOCHWORM_TRAVEL)) return false
			if (blockState.isBlock(ModBlockTagsProvider.PREVENTS_SCOOCHWORM_WANDERING)) return false

			return blockState.isFaceSturdy(level, position, attachmentFace)
		}

		fun finishPlacement(
			worm: ScoochwormEntity,
			level: Level,
			clickedPos: BlockPos,
			clickedFace: Direction,
			player: Player?
		) {
			val clickedSupportDirection = clickedFace.opposite
			if (canAttachTo(worm, level, clickedPos, clickedFace)) {
				worm.attachToSupport(clickedPos, clickedSupportDirection)
			} else {
				for (supportDirection in Direction.entries) {
					val supportPosition = getSupportBlockPosition(worm.position(), supportDirection)
					val attachmentFace = supportDirection.opposite
					if (!canAttachTo(worm, level, supportPosition, attachmentFace)) continue

					worm.attachToSupport(supportPosition, supportDirection)
					break
				}
			}

			if (player == null) return

			val rotation = player.direction.toYRot()
			worm.yRot = rotation
			worm.yRotO = rotation
			worm.yBodyRot = rotation
			worm.yHeadRot = rotation
		}

		fun finishPlacement(
			worm: ScoochwormEntity,
			level: Level,
			spawnPosition: BlockPos,
			facingDirection: Direction
		) {
			for (supportDirection in Direction.entries) {
				val supportPosition = spawnPosition.relative(supportDirection)
				val attachmentFace = supportDirection.opposite
				if (!canAttachTo(worm, level, supportPosition, attachmentFace)) continue

				worm.attachToSupport(supportPosition, supportDirection)
				break
			}

			val rotation = getMovementYaw(facingDirection, worm.supportDirection)
			worm.yRot = rotation
			worm.yRotO = rotation
			worm.yBodyRot = rotation
			worm.yHeadRot = rotation
		}

		private fun canAttachTo(
			worm: ScoochwormEntity,
			level: Level,
			position: BlockPos,
			attachmentFace: Direction
		): Boolean {
			return supportsScoochwormTravel(worm, level, position, attachmentFace)
				|| supportsFreeTravel(level, position, attachmentFace)
		}

		fun getSupportBlockPosition(
			position: Vec3,
			supportDirection: Direction
		): BlockPos {
			// Entity positions are at their feet. Probe outward from the center far enough to
			// land just inside the block that would be supporting this side of the worm.
			val center = position.add(0.0, SIZE / 2.0, 0.0)
			val probe = center.add(
				supportDirection.normal.toVec3()
					.scale(SIZE / 2.0 + SUPPORT_PROBE_DISTANCE)
			)

			return BlockPos.containing(probe)
		}

		fun getMovementYaw(
			movementDirection: Direction,
			supportDirection: Direction
		): Float {
			if (movementDirection.axis != Direction.Axis.Y) {
				return movementDirection.toYRot()
			}

			val movingUp = movementDirection == Direction.UP

			return when (supportDirection) {
				Direction.NORTH -> if (movingUp) 180f else 0f
				Direction.SOUTH -> if (movingUp) 0f else 180f
				Direction.WEST -> if (movingUp) 90f else -90f
				Direction.EAST -> if (movingUp) -90f else 90f
				Direction.UP, Direction.DOWN -> 0f
			}
		}

		fun createAttributes(): AttributeSupplier {
			return createMobAttributes()
				.add(Attributes.MAX_HEALTH, 10.0)
				.add(Attributes.MOVEMENT_SPEED, 0.2)
				.build()
		}
	}
}