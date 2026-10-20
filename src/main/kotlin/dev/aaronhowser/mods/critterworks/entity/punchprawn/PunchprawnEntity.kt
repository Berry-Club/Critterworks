package dev.aaronhowser.mods.critterworks.entity.punchprawn

import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isClientSide
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isFluid
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isItem
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isServerSide
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.tell
import dev.aaronhowser.mods.critterworks.entity.punchprawn.control.PunchprawnMoveControl
import dev.aaronhowser.mods.critterworks.entity.punchprawn.goal.PunchprawnFollowOwnerGoal
import dev.aaronhowser.mods.critterworks.entity.punchprawn.goal.PunchprawnSitGoal
import dev.aaronhowser.mods.critterworks.entity.punchprawn.goal.PunchprawnWanderGoal
import dev.aaronhowser.mods.critterworks.registry.ModItems
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
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
import net.neoforged.neoforge.fluids.FluidType
import software.bernie.geckolib.animatable.GeoEntity
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.animatable.instance.SingletonAnimatableInstanceCache
import software.bernie.geckolib.animation.AnimatableManager
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
		if (isOrderedToSit && isInWater) {
			deltaMovement = Vec3.ZERO
			return
		}

		if (!isInWater) {
			super.travel(travelVector)
			return
		}

		moveRelative(speed, travelVector)
		move(MoverType.SELF, deltaMovement)
		deltaMovement = deltaMovement.scale(0.9)

		if (target == null) {
			deltaMovement = deltaMovement.add(0.0, -0.005, 0.0)
		}
	}

	override fun checkSpawnObstruction(level: LevelReader): Boolean {
		return level.isUnobstructed(this)
	}

	override fun isPushedByFluid(type: FluidType): Boolean {
		return false
	}

	override fun isFood(stack: ItemStack): Boolean {
		return stack.isItem(ModItems.PEARL)
	}

	override fun canAttack(target: LivingEntity): Boolean {
		if (isTame && target is Player) return false

		return super.canAttack(target)
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
				tame(player)
				target = null
				navigation.stop()
				level().broadcastEntityEvent(this, EntityEvent.TAMING_SUCCEEDED)
			}

			return InteractionResult.sidedSuccess(isClientSide)
		}

		if (!isTame || !isOwnedBy(player) || !heldStack.isEmpty) {
			return super.mobInteract(player, hand)
		}

		if (isServerSide && player == owner) {
			if (player.isShiftKeyDown) {
				player.tell("sneaking!")
			} else {
				isOrderedToSit = !isOrderedToSit
				target = null
				navigation.stop()
			}
		}

		return InteractionResult.sidedSuccess(isClientSide)
	}

	override fun getBreedOffspring(level: ServerLevel, otherParent: AgeableMob): AgeableMob? {
		return null
	}

	override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar) {}
	override fun getAnimatableInstanceCache(): AnimatableInstanceCache = animatableInstanceCache

	companion object {
		const val WIDTH = 1f
		const val HEIGHT = 1f
		private const val TAMING_SUCCESS_EVENT: Byte = 7

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
	}

}