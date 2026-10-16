package dev.aaronhowser.mods.critterworks.entity.scoochworm.goal

import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isBlock
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.toDegrees
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.toRadians
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.toVec3
import dev.aaronhowser.mods.critterworks.datagen.tag.ModBlockTagsProvider
import dev.aaronhowser.mods.critterworks.entity.scoochworm.ScoochwormEntity
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.util.Mth
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import java.util.*
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class ScoochwormWanderGoal(
	private val scoochworm: ScoochwormEntity
) : Goal() {

	private var currentSupport: ScoochwormSupport? = null
	private var movementDirection = Vec3.ZERO
	private var homePosition: Vec3? = null
	private var turnSign = 1.0
	private var nextHomeTick = 0
	private var nextTurnSignChangeTick = 0
	private var nextTurnTick = 0
	private var transitionTicks = 0

	init {
		flags = EnumSet.of(Flag.MOVE)
	}

	override fun canUse(): Boolean {
		if (!scoochworm.isTryingToMove) return false

		val currentSupport = findCurrentSupport() ?: return false
		if (isStem(currentSupport)) return false

		movementDirection = getInitialDirection(currentSupport.supportDirection)
		updateHome()
		attachTo(currentSupport)

		return true
	}

	override fun canContinueToUse(): Boolean {
		val currentSupport = this.currentSupport ?: return false
		return scoochworm.isTryingToMove && !isStem(currentSupport)
	}

	override fun start() {
		scoochworm.isNoGravity = true
		scoochworm.noPhysics = false
		nextTurnTick = scoochworm.tickCount + TURN_INTERVAL_TICKS
	}

	override fun tick() {
		val currentSupport = this.currentSupport ?: return

		tryTurning(currentSupport)

		val movementSpeed = scoochworm.getAttributeValue(Attributes.MOVEMENT_SPEED)
		val movement = movementDirection.scale(movementSpeed)
		val nextPosition = scoochworm.position().add(movement)

		if (transitionTicks > 0) {
			continueTransition(currentSupport, nextPosition, movement)
			return
		}

		if (hasAvoidedSupport(nextPosition, currentSupport.supportDirection)) {
			redirectFromCollision()
			return
		}

		if (tryTurningUpwards(currentSupport, nextPosition)) return

		val nextSupport = getSupportAt(nextPosition, currentSupport.supportDirection)
		if (nextSupport != null) return moveOntoSupport(nextSupport, movement)

		// Crossing into another block without finding support means we reached an edge and
		// may be able to turn downwards onto the next side.
		val edgeDirection = getCrossedDirection(currentSupport, nextPosition)
		if (edgeDirection == null) {
			if (!move(movement)) {
				redirectFromCollision()
			}
			return
		}

		val downwardTurnSupport = ScoochwormSupport(
			currentSupport.supportPosition,
			edgeDirection.opposite
		)

		if (isFreeSurface(downwardTurnSupport)) {
			if (!transitionTo(downwardTurnSupport, currentSupport.supportDirection, true)) {
				redirectFromCollision()
			}
		} else {
			redirectFromCollision()
		}
	}

	override fun stop() {
		scoochworm.noPhysics = false
		finishTransition()
		currentSupport = null
	}

	private fun turn(supportDirection: Direction) {
		val turnRange = MAXIMUM_ARC_TURN_RADIANS - MINIMUM_ARC_TURN_RADIANS
		val angle = MINIMUM_ARC_TURN_RADIANS + scoochworm.random.nextDouble() * turnRange
		val surfaceNormal = supportDirection.normal.toVec3()

		movementDirection = rotateAroundAxis(
			movementDirection,
			surfaceNormal,
			angle * turnSign
		).normalize()

		steerTowardHome(surfaceNormal)
	}

	private fun steerTowardHome(surfaceNormal: Vec3) {
		val homePosition = homePosition ?: return
		val horizontalOffset = Vec3(
			homePosition.x - scoochworm.x,
			0.0,
			homePosition.z - scoochworm.z
		)
		val horizontalDistance = horizontalOffset.length()
		if (horizontalDistance <= HOME_STEERING_DISTANCE) return

		val directionTowardHome = projectOntoSurface(horizontalOffset, scoochworm.supportDirection)
		if (directionTowardHome.lengthSqr() <= MINIMUM_DIRECTION_LENGTH_SQUARED) return

		val desiredDirection = directionTowardHome.normalize()
		val signedAngle = atan2(
			surfaceNormal.dot(movementDirection.cross(desiredDirection)),
			movementDirection.dot(desiredDirection)
		)
		val steeringStrength = ((horizontalDistance - HOME_STEERING_DISTANCE) / HOME_STEERING_RANGE)
			.coerceIn(0.0, 1.0)
		val steeringAngle = signedAngle.coerceIn(
			-MAXIMUM_HOME_TURN_RADIANS * steeringStrength,
			MAXIMUM_HOME_TURN_RADIANS * steeringStrength
		)

		movementDirection = rotateAroundAxis(
			movementDirection,
			surfaceNormal,
			steeringAngle
		).normalize()
	}

	private fun tryTurning(currentSupport: ScoochwormSupport) {
		if (transitionTicks > 0 || scoochworm.tickCount < nextTurnTick) return

		updateHome()
		updateTurnSign()
		turn(currentSupport.supportDirection)
		nextTurnTick = scoochworm.tickCount + TURN_INTERVAL_TICKS
	}

	private fun updateHome() {
		if (homePosition != null && scoochworm.tickCount < nextHomeTick) return

		homePosition = scoochworm.position()
		nextHomeTick = scoochworm.tickCount + HOME_DURATION_MINIMUM_TICKS +
			scoochworm.random.nextInt(HOME_DURATION_RANGE_TICKS + 1)
	}

	private fun updateTurnSign() {
		if (nextTurnSignChangeTick != 0 && scoochworm.tickCount < nextTurnSignChangeTick) return

		turnSign = if (nextTurnSignChangeTick == 0) {
			if (scoochworm.random.nextBoolean()) 1.0 else -1.0
		} else {
			-turnSign
		}

		nextTurnSignChangeTick = scoochworm.tickCount + TURN_SIGN_DURATION_MINIMUM_TICKS +
			scoochworm.random.nextInt(TURN_SIGN_DURATION_RANGE_TICKS + 1)
	}

	private fun tryTurningUpwards(
		currentSupport: ScoochwormSupport,
		nextPosition: Vec3
	): Boolean {
		val upwardTurnDirection = getUpwardTurnDirection(
			currentSupport,
			nextPosition
		) ?: return false
		val upwardTurnSupport = getUpwardTurnSupport(
			currentSupport,
			upwardTurnDirection
		) ?: return false

		return transitionTo(
			upwardTurnSupport,
			currentSupport.supportDirection.opposite,
			false
		)
	}

	private fun moveOntoSupport(nextSupport: ScoochwormSupport, movement: Vec3) {
		if (isStem(nextSupport)) {
			handOffToStem(nextSupport)
			return
		}

		if (!move(movement)) {
			redirectFromCollision()
			return
		}

		attachTo(nextSupport)
	}

	private fun transitionTo(
		newSupport: ScoochwormSupport,
		newDirection: Direction,
		snapToEntry: Boolean
	): Boolean {
		var destination = getPositionOnSurface(newSupport)
		if (snapToEntry) {
			destination = getPositionAtEntryEdge(destination, newSupport, newDirection)
		}

		if (!canOccupyAt(destination, newSupport.supportDirection)) return false

		movementDirection = curveDirectionOntoSurface(newSupport, newDirection)
		transitionTicks = TRANSITION_TICKS
		scoochworm.isTurningAroundCorner = true
		attachTo(newSupport)
		scoochworm.setPos(destination)
		move(Vec3.ZERO)

		return true
	}

	private fun curveDirectionOntoSurface(
		newSupport: ScoochwormSupport,
		newDirection: Direction
	): Vec3 {
		val newSurfaceNormal = newSupport.supportDirection.normal.toVec3()

		val directionAlongCorner = movementDirection.subtract(
			newSurfaceNormal.scale(movementDirection.dot(newSurfaceNormal))
		)

		// Preserve the part of the old motion aimed into the corner, but redirect it onto
		// the new surface so the turn keeps the same general speed and heading.
		val directionIntoCorner = abs(movementDirection.dot(newSurfaceNormal))

		val directionAroundCorner = newDirection.normal.toVec3()
			.scale(directionIntoCorner)

		return directionAlongCorner.add(directionAroundCorner).normalize()
	}

	private fun continueTransition(
		currentSupport: ScoochwormSupport,
		nextPosition: Vec3,
		movement: Vec3
	) {
		val nextSupport = getSupportAt(nextPosition, currentSupport.supportDirection)
		if (nextSupport != null) {
			if (isStem(nextSupport)) {
				handOffToStem(nextSupport)
				finishTransition()
				return
			}

			attachTo(nextSupport)
		}

		if (!move(movement)) {
			finishTransition()
			redirectFromCollision()
			return
		}

		transitionTicks--
		if (transitionTicks == 0) {
			finishTransition()
		}
	}

	private fun finishTransition() {
		transitionTicks = 0
		scoochworm.isTurningAroundCorner = false
	}

	private fun attachTo(support: ScoochwormSupport) {
		currentSupport = support
		scoochworm.attachToSupport(support.supportPosition, support.supportDirection)
	}

	private fun handOffToStem(stemSupport: ScoochwormSupport) {
		scoochworm.rememberedMovementDirection = movementDirection
		attachTo(stemSupport)
		snapToSurface(stemSupport)
		scoochworm.deltaMovement = Vec3.ZERO
	}

	private fun move(movement: Vec3): Boolean {
		val canMove = canMove(movement)
		scoochworm.deltaMovement = if (canMove) movement else Vec3.ZERO
		scoochworm.yRot = Mth.atan2(-movementDirection.x, movementDirection.z).toDegrees().toFloat()
		scoochworm.yBodyRot = scoochworm.yRot

		return canMove
	}

	private fun redirectFromCollision() {
		val support = currentSupport
		if (support == null) {
			move(Vec3.ZERO)
			return
		}

		val surfaceNormal = support.supportDirection.normal.toVec3()
		val movementSpeed = scoochworm.getAttributeValue(Attributes.MOVEMENT_SPEED)
		val firstTurnSign = if (scoochworm.random.nextBoolean()) 1.0 else -1.0

		var turnDegrees = COLLISION_TURN_STEP_DEGREES
		while (turnDegrees <= MAXIMUM_COLLISION_TURN_DEGREES) {
			val turnRadians = turnDegrees.toRadians()
			val turnSigns = doubleArrayOf(firstTurnSign, -firstTurnSign)

			for (turnSign in turnSigns) {
				val direction = rotateAroundAxis(
					movementDirection,
					surfaceNormal,
					turnRadians * turnSign
				).normalize()

				if (!canMove(direction.scale(movementSpeed))) continue

				movementDirection = direction
				move(Vec3.ZERO)
				return
			}

			turnDegrees += COLLISION_TURN_STEP_DEGREES
		}

		move(Vec3.ZERO)
	}

	private fun canMove(movement: Vec3): Boolean {
		if (movement == Vec3.ZERO) return true

		val destination = scoochworm.position().add(movement)
		if (hasAvoidedSupport(destination, scoochworm.supportDirection)) return false

		val destinationBounds = scoochworm.boundingBox
			.move(movement)
			.deflate(COLLISION_TOLERANCE)

		return canOccupy(destinationBounds)
	}

	private fun canOccupyAt(position: Vec3, supportDirection: Direction): Boolean {
		if (hasAvoidedSupport(position, supportDirection)) return false

		return canOccupy(getBoundsAt(position))
	}

	private fun hasAvoidedSupport(
		position: Vec3,
		supportDirection: Direction
	): Boolean {
		val supportProbe = supportDirection.normal.toVec3()
			.scale(SUPPORT_PROBE_DISTANCE)
		val supportBounds = getBoundsAt(position)
			.expandTowards(supportProbe)

		return containsAvoidedBlock(supportBounds)
	}

	private fun canOccupy(bounds: AABB): Boolean {
		if (!scoochworm.level().noCollision(scoochworm, bounds)) return false
		return !containsAvoidedBlock(bounds)
	}

	private fun getBoundsAt(position: Vec3): AABB {
		val offset = position.subtract(scoochworm.position())
		return scoochworm.boundingBox
			.move(offset)
			.deflate(COLLISION_TOLERANCE)
	}

	private fun containsAvoidedBlock(bounds: AABB): Boolean {
		val minimumPosition = BlockPos.containing(bounds.minX, bounds.minY, bounds.minZ)
		val maximumPosition = BlockPos.containing(bounds.maxX, bounds.maxY, bounds.maxZ)

		for (position in BlockPos.betweenClosed(minimumPosition, maximumPosition)) {
			val blockState = scoochworm.level().getBlockState(position)
			if (blockState.isBlock(ModBlockTagsProvider.PREVENTS_SCOOCHWORM_WANDERING)) {
				return true
			}
		}

		return false
	}

	private fun getInitialDirection(supportDirection: Direction): Vec3 {
		val minimumDirectionLengthSquared = 0.000001

		val rememberedDirection = scoochworm.rememberedMovementDirection
		if (rememberedDirection != null) {
			val projected = projectOntoSurface(rememberedDirection, supportDirection)
			if (projected.lengthSqr() > minimumDirectionLengthSquared) return projected.normalize()
		}

		val facing = Vec3.directionFromRotation(0f, scoochworm.yRot)
		val projected = projectOntoSurface(facing, supportDirection)
		if (projected.lengthSqr() > minimumDirectionLengthSquared) return projected.normalize()

		return if (supportDirection.axis == Direction.Axis.Y) {
			Vec3(0.0, 0.0, 1.0)
		} else {
			Vec3(0.0, 1.0, 0.0)
		}
	}

	private fun projectOntoSurface(direction: Vec3, supportDirection: Direction): Vec3 {
		val normal = supportDirection.normal.toVec3()
		return direction.subtract(normal.scale(direction.dot(normal)))
	}

	private fun getSupportAt(position: Vec3, supportDirection: Direction): ScoochwormSupport? {
		val supportPosition = ScoochwormEntity.getSupportBlockPosition(
			position,
			supportDirection
		)

		val candidate = ScoochwormSupport(supportPosition, supportDirection)

		return if (isAnySurface(candidate)) candidate else null
	}

	private fun getCrossedDirection(
		currentSupport: ScoochwormSupport,
		nextPosition: Vec3
	): Direction? {
		val nextSupport = ScoochwormEntity.getSupportBlockPosition(
			nextPosition,
			currentSupport.supportDirection
		)

		val difference = nextSupport.subtract(currentSupport.supportPosition)
		if (difference == BlockPos.ZERO) return null

		val direction = Direction.getNearest(
			difference.x.toDouble(),
			difference.y.toDouble(),
			difference.z.toDouble()
		)

		return if (direction.axis == currentSupport.supportDirection.axis) null else direction
	}

	private fun getUpwardTurnDirection(
		currentSupport: ScoochwormSupport,
		nextPosition: Vec3
	): Direction? {
		val supportAxis = currentSupport.supportDirection.axis
		val probeDistance = ScoochwormEntity.SIZE / 2.0 + SUPPORT_PROBE_DISTANCE
		// Push the probe to the leading corner on every axis tangent to the current surface.
		val leadingOffset = Vec3(
			if (supportAxis == Direction.Axis.X) 0.0 else Mth.sign(movementDirection.x) * probeDistance,
			if (supportAxis == Direction.Axis.Y) 0.0 else Mth.sign(movementDirection.y) * probeDistance,
			if (supportAxis == Direction.Axis.Z) 0.0 else Mth.sign(movementDirection.z) * probeDistance
		)
		val leadingPosition = nextPosition.add(leadingOffset)
		return getCrossedDirection(currentSupport, leadingPosition)
	}

	private fun getUpwardTurnSupport(
		currentSupport: ScoochwormSupport,
		crossedDirection: Direction
	): ScoochwormSupport? {
		val upwardTurnPosition = currentSupport.supportPosition
			.relative(currentSupport.supportDirection.opposite)
			.relative(crossedDirection)
		val upwardTurnSupport = ScoochwormSupport(upwardTurnPosition, crossedDirection)
		return if (isAnySurface(upwardTurnSupport)) upwardTurnSupport else null
	}

	private fun findCurrentSupport(): ScoochwormSupport? {
		for (supportDirection in Direction.entries) {
			val candidate = getSupportAt(scoochworm.position(), supportDirection) ?: continue
			return candidate
		}

		return null
	}

	private fun snapToSurface(newSupport: ScoochwormSupport) {
		scoochworm.setPos(getPositionOnSurface(newSupport))
	}

	private fun getPositionOnSurface(newSupport: ScoochwormSupport): Vec3 {
		val blockCenter = newSupport.supportPosition.center
		val normal = newSupport.supportDirection.normal.toVec3()
		val desiredCenter = blockCenter.subtract(
			normal.scale(0.5 + ScoochwormEntity.SIZE / 2.0)
		)
		val currentCenter = scoochworm.position().add(0.0, ScoochwormEntity.SIZE / 2.0, 0.0)
		// Only snap along the surface normal. Tangential coordinates should remain smooth.
		val snappedCenter = Vec3(
			if (normal.x == 0.0) currentCenter.x else desiredCenter.x,
			if (normal.y == 0.0) currentCenter.y else desiredCenter.y,
			if (normal.z == 0.0) currentCenter.z else desiredCenter.z
		)
		return snappedCenter.subtract(0.0, ScoochwormEntity.SIZE / 2.0, 0.0)
	}

	private fun getPositionAtEntryEdge(
		position: Vec3,
		newSupport: ScoochwormSupport,
		movementDirection: Direction
	): Vec3 {
		val blockCenter = newSupport.supportPosition.center
		val currentCenter = position.add(0.0, ScoochwormEntity.SIZE / 2.0, 0.0)

		val surfaceClearance = 0.001
		val entryOffset = 0.5 + ScoochwormEntity.SIZE / 2.0 + surfaceClearance

		val direction = movementDirection.normal
		val entryCenter = Vec3(
			if (direction.x == 0) currentCenter.x else blockCenter.x - direction.x * entryOffset,
			if (direction.y == 0) currentCenter.y else blockCenter.y - direction.y * entryOffset,
			if (direction.z == 0) currentCenter.z else blockCenter.z - direction.z * entryOffset
		)

		return entryCenter.subtract(0.0, ScoochwormEntity.SIZE / 2.0, 0.0)
	}

	private fun isStem(surface: ScoochwormSupport): Boolean {
		return ScoochwormEntity.supportsScoochwormTravel(
			scoochworm,
			scoochworm.level(),
			surface.supportPosition,
			surface.supportDirection.opposite
		)
	}

	private fun isFreeSurface(surface: ScoochwormSupport): Boolean {
		return ScoochwormEntity.supportsFreeTravel(
			scoochworm.level(),
			surface.supportPosition,
			surface.supportDirection.opposite
		)
	}

	private fun isAnySurface(surface: ScoochwormSupport): Boolean {
		return isStem(surface) || isFreeSurface(surface)
	}

	private fun rotateAroundAxis(vector: Vec3, axis: Vec3, angle: Double): Vec3 {
		val cosine = cos(angle)
		val sine = sin(angle)
		// Rodrigues' formula lets the same wandering turn work on every surface orientation.
		return vector.scale(cosine)
			.add(axis.cross(vector).scale(sine))
			.add(axis.scale(axis.dot(vector) * (1.0 - cosine)))
	}

	companion object {
		private const val TURN_INTERVAL_TICKS = 20
		private const val HOME_DURATION_MINIMUM_TICKS = 1200
		private const val HOME_DURATION_RANGE_TICKS = 1200
		private const val TURN_SIGN_DURATION_MINIMUM_TICKS = 300
		private const val TURN_SIGN_DURATION_RANGE_TICKS = 600
		private const val TRANSITION_TICKS = 4
		private const val HOME_STEERING_DISTANCE = 12.0
		private const val HOME_STEERING_RANGE = 12.0
		private const val MINIMUM_DIRECTION_LENGTH_SQUARED = 0.000001
		private val MINIMUM_ARC_TURN_RADIANS = 8.0.toRadians()
		private val MAXIMUM_ARC_TURN_RADIANS = 16.0.toRadians()
		private val MAXIMUM_HOME_TURN_RADIANS = 35.0.toRadians()
		private const val SUPPORT_PROBE_DISTANCE = 0.05
		private const val COLLISION_TOLERANCE = 0.001
		private const val COLLISION_TURN_STEP_DEGREES = 5.0
		private const val MAXIMUM_COLLISION_TURN_DEGREES = 90.0
	}
}