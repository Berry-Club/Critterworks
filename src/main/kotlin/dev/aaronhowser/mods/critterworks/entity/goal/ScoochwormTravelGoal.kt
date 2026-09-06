package dev.aaronhowser.mods.critterworks.entity.goal

import dev.aaronhowser.mods.aaron.misc.AaronExtensions.toVec3
import dev.aaronhowser.mods.critterworks.block.CritterCageBlock
import dev.aaronhowser.mods.critterworks.block.base.ScoochwormTravelBlock
import dev.aaronhowser.mods.critterworks.block_entity.CritterCageBlockEntity
import dev.aaronhowser.mods.critterworks.entity.ScoochwormEntity
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.phys.Vec3
import java.util.*

class ScoochwormTravelGoal(
	private val scoochworm: ScoochwormEntity
) : Goal() {

	private var currentSupport: ScoochwormSupport? = null
	private var movementDirection: Direction? = null
	private var nextSupport: ScoochwormSupport? = null
	private var cornerTarget: Vec3? = null
	private var cornerExitDirection: Direction? = null

	init {
		flags = EnumSet.of(Flag.MOVE)
	}

	override fun canUse(): Boolean {
		if (!scoochworm.isTryingToMove) return false

		val support = findCurrentSupport() ?: return false
		scoochworm.attachToSupport(support.supportPosition, support.supportDirection)

		val direction = getMovementDirection(support.supportDirection)
		currentSupport = support
		val destination = chooseNextSupport(support, direction)
		if (destination == null) {
			tryEnterCage(direction)
			return false
		}

		startMovingTo(support, destination, direction)
		return true
	}

	override fun canContinueToUse(): Boolean {
		return scoochworm.isTryingToMove && nextSupport != null
	}

	override fun start() {
		scoochworm.isNoGravity = true
	}

	override fun tick() {
		val destination = nextSupport ?: return
		val direction = movementDirection ?: return

		val targetPosition = cornerTarget ?: getPositionOnSupport(destination)
		val displacement = scoochworm.position().vectorTo(targetPosition)
		// Only check how far the worm has left to move along its current path. Sideways
		// distance does not matter because the worm moves around a corner one step at a time.
		val distanceToTarget = displacement
			.dot(direction.normal.toVec3())

		val targetDistance = 0.001
		if (distanceToTarget <= targetDistance) {
			if (cornerTarget == null) {
				arriveAtSupport(destination, direction)
			} else {
				arriveAtCorner(destination, targetPosition)
			}
		}

		requestMovement()
	}

	private fun arriveAtSupport(destination: ScoochwormSupport, direction: Direction) {
		scoochworm.setPos(getPositionOnSupport(destination))
		scoochworm.attachToSupport(destination.supportPosition, destination.supportDirection)
		currentSupport = destination

		val followingSupport = chooseNextSupport(destination, direction)
		if (followingSupport == null) {
			nextSupport = null
			scoochworm.deltaMovement = Vec3.ZERO
			tryEnterCage(direction)
			return
		}

		startMovingTo(destination, followingSupport, direction)
	}

	private fun arriveAtCorner(destination: ScoochwormSupport, position: Vec3) {
		scoochworm.setPos(position)
		scoochworm.attachToSupport(destination.supportPosition, destination.supportDirection)
		cornerTarget = null
		movementDirection = cornerExitDirection
		cornerExitDirection = null
	}

	private fun startMovingTo(
		from: ScoochwormSupport,
		to: ScoochwormSupport,
		approachDirection: Direction
	) {
		nextSupport = to

		if (from.supportDirection == to.supportDirection) {
			cornerTarget = null
			cornerExitDirection = null
			movementDirection = getDirectionToSupport(from, to, approachDirection)
			return
		}

		val fromPosition = getPositionOnSupport(from)
		val toPosition = getPositionOnSupport(to)

		// When turning upwards or downwards, first move to the corner where the two paths
		// meet. From there, move onto the new side. This stops the worm from taking a
		// diagonal shortcut through the blocks.
		val corner = when (approachDirection.axis) {
			Direction.Axis.X -> Vec3(toPosition.x, fromPosition.y, fromPosition.z)
			Direction.Axis.Y -> Vec3(fromPosition.x, toPosition.y, fromPosition.z)
			Direction.Axis.Z -> Vec3(fromPosition.x, fromPosition.y, toPosition.z)
		}

		cornerTarget = corner
		cornerExitDirection = getDirectionBetween(corner, toPosition)
		movementDirection = approachDirection
	}

	override fun stop() {
		nextSupport = null
		currentSupport = null
		movementDirection = null
		cornerTarget = null
		cornerExitDirection = null
		scoochworm.noPhysics = false
	}

	private fun chooseNextSupport(support: ScoochwormSupport, forward: Direction): ScoochwormSupport? {
		return tryMoveForward(support, forward)
			?: tryTurnAlongSurface(support, forward)
			?: tryTurnUpwards(support, forward)
			?: tryTurnDownwards(support, forward)
	}

	private fun tryEnterCage(forward: Direction): Boolean {
		val wormCenter = scoochworm.position().add(0.0, ScoochwormEntity.SIZE / 2.0, 0.0)
		val cagePosition = BlockPos.containing(wormCenter).relative(forward)
		val level = scoochworm.level()
		val cageState = level.getBlockState(cagePosition)
		if (cageState.block !is CritterCageBlock) return false
		if (cageState.getValue(CritterCageBlock.FORWARD) != forward.opposite) return false
		if (!cageState.getValue(CritterCageBlock.OPEN)) return false

		val cage = level.getBlockEntity(cagePosition)
		if (cage !is CritterCageBlockEntity || cage.hasEntity) return false

		return cage.tryCapture(scoochworm)
	}

	private fun tryMoveForward(
		support: ScoochwormSupport,
		forward: Direction
	): ScoochwormSupport? {
		val forwardSupport = support.copy(
			supportPosition = support.supportPosition.relative(forward)
		)

		return if (canMoveOnto(forwardSupport)) forwardSupport else null
	}

	private fun tryTurnAlongSurface(
		support: ScoochwormSupport,
		forward: Direction
	): ScoochwormSupport? {
		val left = turnAlongSurface(forward, support.supportDirection, false)
		val right = turnAlongSurface(forward, support.supportDirection, true)

		val leftSupport = support.copy(
			supportPosition = support.supportPosition.relative(left)
		)
		val rightSupport = support.copy(
			supportPosition = support.supportPosition.relative(right)
		)

		val canTurnLeft = canMoveOnto(leftSupport)
		val canTurnRight = canMoveOnto(rightSupport)

		return when {
			canTurnLeft && canTurnRight -> if (scoochworm.random.nextBoolean()) leftSupport else rightSupport
			canTurnLeft -> leftSupport
			canTurnRight -> rightSupport
			else -> null
		}
	}

	private fun tryTurnUpwards(
		support: ScoochwormSupport,
		forward: Direction
	): ScoochwormSupport? {
		val upperForwardPosition = support.supportPosition
			.relative(forward)
			.relative(support.supportDirection.opposite)

		val upwardTurnSupport = ScoochwormSupport(upperForwardPosition, forward)

		if (canMoveOnto(upwardTurnSupport)) return upwardTurnSupport

		val nearbyUpwardTurnSupport = ScoochwormSupport(
			support.supportPosition.relative(support.supportDirection.opposite),
			forward
		)

		return if (canMoveOnto(nearbyUpwardTurnSupport)) nearbyUpwardTurnSupport else null
	}

	private fun tryTurnDownwards(
		support: ScoochwormSupport,
		forward: Direction
	): ScoochwormSupport? {
		val downwardTurnSupport = ScoochwormSupport(
			support.supportPosition,
			forward.opposite
		)

		if (!isStemSupport(downwardTurnSupport)) return null

		val forwardPosition = support.supportPosition.relative(forward)
		if (!hasNoCollision(forwardPosition)) return null

		val upperForwardPosition = forwardPosition.relative(support.supportDirection.opposite)
		if (!hasNoCollision(upperForwardPosition)) return null

		return downwardTurnSupport
	}

	private fun requestMovement() {
		val current = currentSupport ?: return
		val destination = nextSupport ?: return
		val direction = movementDirection ?: return
		val position = cornerTarget ?: getPositionOnSupport(destination)

		scoochworm.stemMoveControl.setWantedPosition(
			position.x,
			position.y,
			position.z,
			direction,
			current.supportDirection != destination.supportDirection,
			1.0
		)
	}

	private fun findCurrentSupport(): ScoochwormSupport? {
		for (supportDirection in Direction.entries) {
			val stem = BlockPos.containing(
				scoochworm.position()
					.add(supportDirection.normal.toVec3())
			)

			if (isStemSupport(stem, supportDirection)) {
				return ScoochwormSupport(stem, supportDirection)
			}
		}

		return null
	}

	private fun getInitialTravelDirection(supportDirection: Direction): Direction {
		val rememberedDirection = scoochworm.rememberedMovementDirection
		if (rememberedDirection != null) {
			scoochworm.rememberedMovementDirection = null
			return Direction.entries
				.filter { it.axis != supportDirection.axis }
				.maxBy { direction ->
					rememberedDirection.dot(direction.normal.toVec3())
				}
		}

		val horizontal = Direction.fromYRot(scoochworm.yRot.toDouble())
		if (horizontal.axis != supportDirection.axis) return horizontal
		return if (supportDirection.axis == Direction.Axis.Y) Direction.NORTH else Direction.UP
	}

	private fun getMovementDirection(supportDirection: Direction): Direction {
		val currentDirection = movementDirection
		if (
			currentDirection != null
			&& currentDirection.axis != supportDirection.axis
		) {
			return currentDirection
		}

		return getInitialTravelDirection(supportDirection)
	}

	private fun canMoveOnto(support: ScoochwormSupport): Boolean {
		val current = currentSupport

		if (current != null) {
			val towardsDirection = getDirectionToSupport(
				current,
				support,
				movementDirection ?: Direction.NORTH
			)

			if (!canExitCurrentSupport(current, towardsDirection)) return false
			if (!canEnterSupport(support, towardsDirection)) return false
		}

		if (!isStemSupport(support)) return false

		val position = getPositionOnSupport(support)
		// Move a copy of the worm's hitbox to the target and make sure it fits there.
		val bounds = scoochworm.boundingBox.move(
			position.x - scoochworm.x,
			position.y - scoochworm.y,
			position.z - scoochworm.z
		)

		return scoochworm.level().noCollision(scoochworm, bounds)
	}

	private fun canExitCurrentSupport(current: ScoochwormSupport, towardsDirection: Direction): Boolean {
		val state = scoochworm.level().getBlockState(current.supportPosition)
		val block = state.block
		if (block !is ScoochwormTravelBlock) return true

		return block.canDetachFromBlock(
			state,
			scoochworm,
			scoochworm.level(),
			current.supportPosition,
			current.supportDirection,
			towardsDirection
		)
	}

	private fun canEnterSupport(
		destination: ScoochwormSupport,
		towardsDirection: Direction
	): Boolean {
		val state = scoochworm.level().getBlockState(destination.supportPosition)
		val block = state.block
		if (block !is ScoochwormTravelBlock) return true

		return block.canAttachToBlock(
			state,
			scoochworm,
			scoochworm.level(),
			destination.supportPosition,
			destination.supportDirection,
			towardsDirection.opposite
		)
	}

	private fun isStemSupport(support: ScoochwormSupport): Boolean {
		return isStemSupport(support.supportPosition, support.supportDirection)
	}

	private fun isStemSupport(position: BlockPos, supportDirection: Direction): Boolean {
		return ScoochwormEntity.supportsScoochwormTravel(
			scoochworm,
			scoochworm.level(),
			position,
			supportDirection.opposite
		)
	}

	private fun hasNoCollision(position: BlockPos): Boolean {
		val level = scoochworm.level()
		return level.getBlockState(position)
			.getCollisionShape(level, position)
			.isEmpty
	}

	private fun getPositionOnSupport(support: ScoochwormSupport): Vec3 {
		val blockCenter = support.supportPosition.center
		val center = blockCenter.subtract(
			support.supportDirection.normal
				.toVec3()
				.scale(0.5 + ScoochwormEntity.SIZE / 2.0)
		)

		return center.subtract(0.0, ScoochwormEntity.SIZE / 2.0, 0.0)
	}

	private fun getDirectionToSupport(
		from: ScoochwormSupport,
		to: ScoochwormSupport,
		previousDirection: Direction
	): Direction {
		if (from.supportDirection != to.supportDirection) {
			return if (to.supportDirection == previousDirection) {
				from.supportDirection.opposite
			} else {
				from.supportDirection
			}
		}

		val displacement = getPositionOnSupport(from).vectorTo(getPositionOnSupport(to))
		return Direction.getNearest(displacement.x, displacement.y, displacement.z)
	}

	private fun getDirectionBetween(from: Vec3, to: Vec3): Direction {
		val displacement = from.vectorTo(to)
		return Direction.getNearest(displacement.x, displacement.y, displacement.z)
	}

	private fun turnAlongSurface(
		direction: Direction,
		supportDirection: Direction,
		clockwise: Boolean
	): Direction {
		val first = if (clockwise) supportDirection.normal else direction.normal
		val second = if (clockwise) direction.normal else supportDirection.normal

		// Use the path and the side holding the worm to find left or right. This works the
		// same way on a floor, wall, or ceiling.
		return Direction.fromDelta(
			first.y * second.z - first.z * second.y,
			first.z * second.x - first.x * second.z,
			first.x * second.y - first.y * second.x
		) ?: direction
	}

}
