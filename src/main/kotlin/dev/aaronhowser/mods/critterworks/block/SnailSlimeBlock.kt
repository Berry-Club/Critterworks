package dev.aaronhowser.mods.critterworks.block

import com.mojang.serialization.MapCodec
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.MultifaceBlock
import net.minecraft.world.level.block.MultifaceSpreader
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.material.MapColor
import net.minecraft.world.phys.Vec3

class SnailSlimeBlock(properties: BlockBehaviour.Properties) : MultifaceBlock(properties) {

	constructor() : this(
		Properties
			.ofFullCopy(Blocks.HONEY_BLOCK)
			.mapColor(MapColor.COLOR_BROWN)
	)

	private val spreader = MultifaceSpreader(this)

	override fun codec(): MapCodec<out SnailSlimeBlock> = CODEC

	override fun getSpreader(): MultifaceSpreader = spreader

	override fun fallOn(
		level: Level,
		state: BlockState,
		position: BlockPos,
		entity: Entity,
		fallDistance: Float
	) {
		if (!hasFace(state, Direction.DOWN)) {
			super.fallOn(level, state, position, entity, fallDistance)
			return
		}

		entity.playSound(SoundEvents.HONEY_BLOCK_SLIDE, 1.0f, 1.0f)

		if (entity.causeFallDamage(fallDistance, 0.2f, level.damageSources().fall())) {
			val soundType = state.getSoundType(level, position, entity)
			entity.playSound(
				soundType.fallSound,
				soundType.volume * 0.5f,
				soundType.pitch * 0.75f
			)
		}
	}

	override fun entityInside(state: BlockState, level: Level, position: BlockPos, entity: Entity) {
		if (isSlidingDown(state, position, entity)) {
			slowSlide(entity)

			if (level.random.nextInt(5) == 0) {
				entity.playSound(SoundEvents.HONEY_BLOCK_SLIDE, 1.0f, 1.0f)
			}
		}

		super.entityInside(state, level, position, entity)
	}

	private fun isSlidingDown(state: BlockState, position: BlockPos, entity: Entity): Boolean {
		if (entity.onGround()) return false
		if (entity.y > position.y + 1.0 - COLLISION_EPSILON) return false
		if (entity.deltaMovement.y >= MINIMUM_SLIDE_SPEED) return false

		val entityBounds = entity.boundingBox
		val minimumX = position.x + FACE_THICKNESS
		val maximumX = position.x + 1.0 - FACE_THICKNESS
		val minimumZ = position.z + FACE_THICKNESS
		val maximumZ = position.z + 1.0 - FACE_THICKNESS

		if (hasFace(state, Direction.WEST) && entityBounds.minX <= minimumX + COLLISION_EPSILON) return true
		if (hasFace(state, Direction.EAST) && entityBounds.maxX >= maximumX - COLLISION_EPSILON) return true
		if (hasFace(state, Direction.NORTH) && entityBounds.minZ <= minimumZ + COLLISION_EPSILON) return true
		if (hasFace(state, Direction.SOUTH) && entityBounds.maxZ >= maximumZ - COLLISION_EPSILON) return true

		return false
	}

	private fun slowSlide(entity: Entity) {
		val movement = entity.deltaMovement

		if (movement.y < SLIDE_SPEED_THRESHOLD) {
			val horizontalMultiplier = MAXIMUM_SLIDE_SPEED / -movement.y
			entity.deltaMovement = Vec3(
				movement.x * horizontalMultiplier,
				-MAXIMUM_SLIDE_SPEED,
				movement.z * horizontalMultiplier
			)
		} else {
			entity.deltaMovement = Vec3(movement.x, -MAXIMUM_SLIDE_SPEED, movement.z)
		}

		entity.resetFallDistance()
	}

	companion object {
		private const val FACE_THICKNESS = 1.0 / 16.0
		private const val COLLISION_EPSILON = 1.0E-7
		private const val MINIMUM_SLIDE_SPEED = -0.08
		private const val SLIDE_SPEED_THRESHOLD = -0.13
		private const val MAXIMUM_SLIDE_SPEED = 0.05

		val CODEC: MapCodec<SnailSlimeBlock> = simpleCodec(::create)

		private fun create(properties: BlockBehaviour.Properties): SnailSlimeBlock {
			return SnailSlimeBlock(properties)
		}
	}

}