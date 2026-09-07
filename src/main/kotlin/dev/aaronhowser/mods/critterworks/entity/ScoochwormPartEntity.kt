package dev.aaronhowser.mods.critterworks.entity

import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isClientSide
import dev.aaronhowser.mods.critterworks.entity.attachment.ScoochwormAttachment
import dev.aaronhowser.mods.critterworks.entity.attachment.data.NoAttachmentData
import dev.aaronhowser.mods.critterworks.entity.attachment.data.SyncedAttachmentData
import dev.aaronhowser.mods.critterworks.entity.data.ScoochwormSegment
import dev.aaronhowser.mods.critterworks.entity.data.WormColor
import dev.aaronhowser.mods.critterworks.registry.ModEntityDataSerializers
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityDimensions
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.material.PushReaction
import net.minecraft.world.phys.Vec3
import net.neoforged.neoforge.items.IItemHandler
import software.bernie.geckolib.animatable.GeoEntity
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.animatable.instance.SingletonAnimatableInstanceCache
import software.bernie.geckolib.animation.AnimatableManager

class ScoochwormPartEntity(
	entityType: EntityType<ScoochwormPartEntity>,
	level: Level
) : Entity(entityType, level), GeoEntity {

	private val animatableInstanceCache = SingletonAnimatableInstanceCache(this)
	private var lerpSteps = 0
	private var lerpX = 0.0
	private var lerpY = 0.0
	private var lerpZ = 0.0
	private var ownerSegment: ScoochwormSegment? = null

	init {
		noPhysics = true
	}

	// Parent

	var parentId: Int
		get() = entityData.get(DATA_PARENT_ID)
		set(value) = entityData.set(DATA_PARENT_ID, value)

	var partIndex: Int
		get() = entityData.get(DATA_PART_INDEX)
		set(value) = entityData.set(DATA_PART_INDEX, value)

	var attachmentData: SyncedAttachmentData
		get() = entityData.get(DATA_ATTACHMENT_DATA)
		set(value) = entityData.set(DATA_ATTACHMENT_DATA, value)

	var color: WormColor
		get() = WormColor.fromOrdinal(entityData.get(DATA_COLOR))
		set(value) = entityData.set(DATA_COLOR, value.ordinal)

	var supportDirection: Direction
		get() = entityData.get(DATA_BOTTOM_DIRECTION)
		private set(value) = entityData.set(DATA_BOTTOM_DIRECTION, value)

	fun getScoochworm(): ScoochwormEntity? {
		return level().getEntity(parentId) as? ScoochwormEntity
	}

	fun getItemHandler(): IItemHandler? {
		return ownerSegment?.getItemHandler()
	}

	fun getAttachment(): ScoochwormAttachment? {
		return ownerSegment?.getAttachment()
	}

	fun getPartInFront(): Entity? {
		if (partIndex == 0) return getScoochworm()

		val nearbyParts = level().getEntitiesOfClass(
			ScoochwormPartEntity::class.java,
			boundingBox.inflate(ScoochwormEntity.PART_SPACING * 2)
		)

		for (nearbyPart in nearbyParts) {
			if (nearbyPart.parentId != parentId) continue
			if (nearbyPart.partIndex != partIndex - 1) continue

			return nearbyPart
		}

		return null
	}

	fun attachTo(
		parentEntity: ScoochwormEntity,
		partIndex: Int,
		attachmentData: SyncedAttachmentData,
		ownerSegment: ScoochwormSegment
	) {
		this.parentId = parentEntity.id
		this.partIndex = partIndex
		this.attachmentData = attachmentData
		this.ownerSegment = ownerSegment
		color = parentEntity.color
	}

	// Movement

	fun moveAlongPath(pathPosition: Vec3, newSupportDirection: Direction) {
		val displacement = position().vectorTo(pathPosition)
		var movementYaw = yRot

		if (displacement.lengthSqr() >= MINIMUM_MOVEMENT_DISTANCE_SQUARED) {
			val movementDirection = Direction.getNearest(
				displacement.x,
				displacement.y,
				displacement.z
			)

			movementYaw = ScoochwormEntity.getMovementYaw(
				movementDirection,
				newSupportDirection
			)
		}

		setPos(pathPosition)
		supportDirection = newSupportDirection
		yRot = movementYaw
	}

	override fun getPassengerAttachmentPoint(
		entity: Entity,
		dimensions: EntityDimensions,
		partialTick: Float
	): Vec3 {
		val attachmentDistance = super.getPassengerAttachmentPoint(
			entity,
			dimensions,
			partialTick
		).y

		val attachmentDirection = supportDirection.opposite

		return Vec3(
			attachmentDirection.stepX * attachmentDistance,
			attachmentDirection.stepY * attachmentDistance,
			attachmentDirection.stepZ * attachmentDistance
		)
	}

	// Lifecycle

	override fun tick() {
		super.tick()

		if (isClientSide) {
			val scoochworm = getScoochworm()
			if (scoochworm != null) {
				ownerSegment = scoochworm.bindClientBodyPart(this)
			}

			ownerSegment?.clientTick()
			tickInterpolation()
			return
		}

		val scoochworm = getScoochworm()
		if (scoochworm == null || scoochworm.isRemoved) {
			discard()
		}
	}

	// Interpolation

	override fun getPistonPushReaction(): PushReaction = PushReaction.IGNORE

	override fun lerpTo(
		x: Double,
		y: Double,
		z: Double,
		yRot: Float,
		xRot: Float,
		lerpSteps: Int
	) {
		lerpX = x
		lerpY = y
		lerpZ = z
		this.lerpSteps = lerpSteps

		this.yRot = yRot
		this.xRot = xRot
		yRotO = yRot
		xRotO = xRot
	}

	// I don't really understand this, but it's similar to how it's done in LivingEntity
	// Or something.
	private fun tickInterpolation() {
		if (lerpSteps <= 0) return

		val x = x + (lerpX - x) / lerpSteps
		val y = y + (lerpY - y) / lerpSteps
		val z = z + (lerpZ - z) / lerpSteps

		lerpSteps--
		setPos(x, y, z)
	}

	// Collision

	override fun hurt(damageSource: DamageSource, amount: Float): Boolean {
		val scoochworm = getScoochworm() ?: return false
		return scoochworm.hurt(damageSource, amount)
	}

	override fun interact(player: Player, hand: InteractionHand): InteractionResult {
		val scoochworm = getScoochworm() ?: return InteractionResult.PASS
		return scoochworm.interactWithPart(player, hand, partIndex)
	}

	override fun canCollideWith(entity: Entity): Boolean {
		return when (entity) {
			is ScoochwormEntity -> entity.id != parentId
			is ScoochwormPartEntity -> entity.parentId != parentId
			else -> !entity.isPassenger
		}
	}

	override fun isPickable(): Boolean = true
	override fun isPushable(): Boolean = false

	// Entity data

	override fun defineSynchedData(builder: SynchedEntityData.Builder) {
		builder.define(DATA_PARENT_ID, NO_PARENT)
		builder.define(DATA_PART_INDEX, 0)
		builder.define(DATA_ATTACHMENT_DATA, NoAttachmentData)
		builder.define(DATA_BOTTOM_DIRECTION, Direction.DOWN)
		builder.define(DATA_COLOR, WormColor.GREEN.ordinal)
	}

	override fun readAdditionalSaveData(tag: CompoundTag) {}
	override fun addAdditionalSaveData(tag: CompoundTag) {}

	override fun shouldBeSaved(): Boolean = false

	// Animation

	override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar) {}

	override fun getAnimatableInstanceCache(): AnimatableInstanceCache = animatableInstanceCache

	companion object {
		private const val NO_PARENT = -1
		private const val MINIMUM_MOVEMENT_DISTANCE_SQUARED = 0.000001
		private val DATA_PARENT_ID: EntityDataAccessor<Int> =
			SynchedEntityData.defineId(ScoochwormPartEntity::class.java, EntityDataSerializers.INT)

		private val DATA_PART_INDEX: EntityDataAccessor<Int> =
			SynchedEntityData.defineId(ScoochwormPartEntity::class.java, EntityDataSerializers.INT)

		private val DATA_ATTACHMENT_DATA: EntityDataAccessor<SyncedAttachmentData> =
			SynchedEntityData.defineId(
				ScoochwormPartEntity::class.java,
				ModEntityDataSerializers.SCOOCHWORM_ATTACHMENT_DATA.get()
			)

		private val DATA_BOTTOM_DIRECTION: EntityDataAccessor<Direction> =
			SynchedEntityData.defineId(
				ScoochwormPartEntity::class.java,
				EntityDataSerializers.DIRECTION
			)

		private val DATA_COLOR: EntityDataAccessor<Int> =
			SynchedEntityData.defineId(
				ScoochwormPartEntity::class.java,
				EntityDataSerializers.INT
			)

	}

}