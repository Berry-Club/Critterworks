package dev.aaronhowser.mods.critterworks.entity.punchprawn

import dev.aaronhowser.mods.aaron.misc.AaronExtensions.getMinimalTag
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isClientSide
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isFluid
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isItem
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isServerSide
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.oneIn
import dev.aaronhowser.mods.critterworks.entity.punchprawn.control.PunchprawnMoveControl
import dev.aaronhowser.mods.critterworks.entity.punchprawn.goal.PunchprawnFollowOwnerGoal
import dev.aaronhowser.mods.critterworks.entity.punchprawn.goal.PunchprawnSitGoal
import dev.aaronhowser.mods.critterworks.entity.punchprawn.goal.PunchprawnWanderGoal
import dev.aaronhowser.mods.critterworks.registry.ModAttachmentTypes
import dev.aaronhowser.mods.critterworks.registry.ModEntityTypes
import dev.aaronhowser.mods.critterworks.registry.ModItems
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.tags.FluidTags
import net.minecraft.util.RandomSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.*
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation
import net.minecraft.world.entity.ai.navigation.PathNavigation
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.ServerLevelAccessor
import net.minecraft.world.level.block.LeavesBlock
import net.minecraft.world.level.pathfinder.PathType
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator
import net.minecraft.world.phys.Vec3
import net.neoforged.neoforge.event.EventHooks
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent
import software.bernie.geckolib.animatable.GeoEntity
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.animatable.instance.SingletonAnimatableInstanceCache
import software.bernie.geckolib.animation.AnimatableManager
import java.util.*
import kotlin.jvm.optionals.getOrNull
import kotlin.math.abs

class PunchprawnEntity(
	entityType: EntityType<out PunchprawnEntity>,
	level: Level
) : TamableAnimal(entityType, level), GeoEntity {

	private val animatableInstanceCache = SingletonAnimatableInstanceCache(this)

	init {
		moveControl = PunchprawnMoveControl(this)
		lookControl = SmoothSwimmingLookControl(this, 10)
		setPathfindingMalus(PathType.WATER, 0f)
	}

	override fun registerGoals() {
		goalSelector.addGoal(1, PunchprawnSitGoal(this))
		goalSelector.addGoal(2, MeleeAttackGoal(this, 1.2, true))
		goalSelector.addGoal(3, PunchprawnFollowOwnerGoal(this, 1.0, 8f, 2f))
		goalSelector.addGoal(4, PunchprawnWanderGoal(this))

		targetSelector.addGoal(1, HurtByTargetGoal(this))
		targetSelector.addGoal(2, NearestAttackableTargetGoal(this, Player::class.java, true))
	}

	override fun createNavigation(level: Level): PathNavigation {
		return AmphibiousPathNavigation(this, level)
	}

	override fun travel(travelVector: Vec3) {
		if (!isControlledByLocalInstance) {
			calculateEntityAnimation(false)
			return
		}

		if (!isInWater) {
			super.travel(travelVector)
			return
		}

		moveRelative(speed, travelVector)
		move(MoverType.SELF, deltaMovement)
		deltaMovement = deltaMovement.scale(0.9)

		if (!isOrderedToSit && target == null) {
			deltaMovement = deltaMovement.add(0.0, -0.005, 0.0)
		}
	}

	override fun checkSpawnObstruction(level: LevelReader): Boolean {
		return level.isUnobstructed(this)
	}

	override fun isFood(stack: ItemStack): Boolean {
		return stack.isItem(ModItems.PEARL)
	}

	override fun canAttack(target: LivingEntity): Boolean {
		if (target is Player) {
			if (isTame) return false
			if (isHoldingPearl(target)) return false
		}

		return super.canAttack(target)
	}

	private fun isHoldingPearl(player: Player): Boolean {
		return player.mainHandItem.isItem(ModItems.PEARL)
			|| player.offhandItem.isItem(ModItems.PEARL)
	}

	override fun tryToTeleportToOwner() {
		val currentOwner = owner ?: return
		val ownerPosition = currentOwner.blockPosition()
		var attempt = 0

		while (attempt < 10) {
			val horizontalOffsetX = random.nextIntBetweenInclusive(-3, 3)
			val horizontalOffsetZ = random.nextIntBetweenInclusive(-3, 3)

			if (abs(horizontalOffsetX) >= 2 || abs(horizontalOffsetZ) >= 2) {
				val verticalOffset = random.nextIntBetweenInclusive(-1, 1)
				val destination = ownerPosition.offset(
					horizontalOffsetX,
					verticalOffset,
					horizontalOffsetZ
				)

				if (tryTeleportTo(destination)) return
			}

			attempt++
		}
	}

	private fun tryTeleportTo(destination: BlockPos): Boolean {
		val destinationPathType = WalkNodeEvaluator.getPathTypeStatic(this, destination)
		val isWalkable = destinationPathType == PathType.WALKABLE
		val isWater = level()
			.getFluidState(destination)
			.isFluid(FluidTags.WATER)

		if (!isWalkable && !isWater) return false
		if (isWalkable && level().getBlockState(destination.below()).block is LeavesBlock) return false

		val positionOffset = destination.subtract(blockPosition())
		if (!level().noCollision(this, boundingBox.move(positionOffset))) return false

		teleportTo(
			destination.x + 0.5,
			destination.y.toDouble(),
			destination.z + 0.5
		)
		navigation.stop()
		return true
	}

	override fun mobInteract(player: Player, hand: InteractionHand): InteractionResult {
		val heldStack = player.getItemInHand(hand)

		if (!isTame && heldStack.isItem(ModItems.PEARL)) {
			if (isServerSide) {
				heldStack.consume(1, player)
				tryToTame(player)
			}

			return InteractionResult.sidedSuccess(isClientSide)
		}

		if (!isTame || !isOwnedBy(player) || !heldStack.isEmpty) {
			return super.mobInteract(player, hand)
		}

		if (isServerSide && player == owner) {
			if (player.isShiftKeyDown && player.getData(ModAttachmentTypes.WORN_PUNCHPRAWN).isEmpty) {
				val tag = getMinimalTag(stripUniqueness = false)
				player.setData(ModAttachmentTypes.WORN_PUNCHPRAWN, Optional.of(tag))
				playSound(SoundEvents.ITEM_FRAME_ADD_ITEM)
				discard()
			} else {
				isOrderedToSit = !isOrderedToSit
				target = null
				navigation.stop()
			}
		}

		return InteractionResult.sidedSuccess(isClientSide)
	}

	private fun tryToTame(player: Player) {
		if (random.oneIn(5) && !EventHooks.onAnimalTame(this, player)) {
			tame(player)
			navigation.stop()
			target = null
			level().broadcastEntityEvent(this, EntityEvent.TAMING_SUCCEEDED)
		} else {
			level().broadcastEntityEvent(this, EntityEvent.TAMING_FAILED)
		}
	}

	override fun getBreedOffspring(level: ServerLevel, otherParent: AgeableMob): AgeableMob? {
		return null
	}

	override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar) {}
	override fun getAnimatableInstanceCache(): AnimatableInstanceCache = animatableInstanceCache

	companion object {
		const val WIDTH = 1f
		const val HEIGHT = 1f

		fun createAttributes(): AttributeSupplier {
			return createMobAttributes()
				.add(Attributes.MOVEMENT_SPEED, 1.2)
				.add(Attributes.ATTACK_DAMAGE, 5.0)
				.add(Attributes.ATTACK_KNOCKBACK, 3.5)
				.build()
		}

		fun checkSpawnRules(
			entityType: EntityType<PunchprawnEntity>,
			level: ServerLevelAccessor,
			spawnType: MobSpawnType,
			position: BlockPos,
			random: RandomSource
		): Boolean {
			return level.getFluidState(position).isFluid(FluidTags.WATER)
		}

		fun tryPlacePunchprawn(event: PlayerInteractEvent.RightClickBlock) {
			if (event.isCanceled) return

			val player = event.entity
			val level = event.level
			val tag = player.getData(ModAttachmentTypes.WORN_PUNCHPRAWN).getOrNull() ?: return
			val clickedPos = event.pos
			val clickedFace = event.face ?: return
			val clickedState = level.getBlockState(clickedPos)

			val spawnPosition = if (clickedState.getCollisionShape(level, clickedPos).isEmpty) {
				clickedPos
			} else {
				clickedPos.relative(clickedFace)
			}

			val entity = PunchprawnEntity(ModEntityTypes.PUNCHPRAWN.get(), level)
			entity.load(tag)
			entity.setPos(spawnPosition.bottomCenter)
			if (!level.noCollision(entity, entity.boundingBox)) return

			if (player.isServerSide) {
				if (!level.addFreshEntity(entity)) return

				player.setData(ModAttachmentTypes.WORN_PUNCHPRAWN, Optional.empty())
				entity.playSound(SoundEvents.ITEM_FRAME_REMOVE_ITEM)
			}

			event.cancellationResult = InteractionResult.sidedSuccess(player.isClientSide)
			event.isCanceled = true
		}
	}

}