package dev.aaronhowser.mods.critterworks.entity.sproutsnail

import dev.aaronhowser.mods.aaron.misc.AaronExtensions.asIngredient
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.chance
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isItem
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isServerSide
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.nextRange
import dev.aaronhowser.mods.critterworks.config.ServerConfig
import dev.aaronhowser.mods.critterworks.entity.sproutsnail.goal.SproutsnailMoveToBonemealableBlockGoal
import dev.aaronhowser.mods.critterworks.handler.snail.EvilSnailHandler
import dev.aaronhowser.mods.critterworks.registry.ModItems
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.AgeableMob
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal
import net.minecraft.world.entity.ai.goal.RandomStrollGoal
import net.minecraft.world.entity.ai.goal.TemptGoal
import net.minecraft.world.entity.animal.Animal
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BonemealableBlock
import net.minecraft.world.level.block.LevelEvent
import net.minecraft.world.level.gameevent.GameEvent
import net.neoforged.neoforge.common.Tags
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent
import software.bernie.geckolib.animatable.GeoEntity
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.animation.AnimatableManager
import software.bernie.geckolib.util.GeckoLibUtil

class SproutsnailEntity(
	entityType: EntityType<out SproutsnailEntity>,
	level: Level
) : Animal(entityType, level), GeoEntity {

	private val animatableInstanceCache = GeckoLibUtil.createInstanceCache(this)
	private var slimeDropTime = random.nextRange(6000, 12001)

	override fun registerGoals() {
		goalSelector.addGoal(1, TemptGoal(this, 1.0, Tags.Items.MUSHROOMS.asIngredient(), false))
		goalSelector.addGoal(2, SproutsnailMoveToBonemealableBlockGoal(this))
		goalSelector.addGoal(3, RandomStrollGoal(this, 1.0))
		goalSelector.addGoal(4, LookAtPlayerGoal(this, Player::class.java, 6f))
		goalSelector.addGoal(5, RandomLookAroundGoal(this))
	}

	override fun isFood(stack: ItemStack): Boolean {
		return stack.isItem(Tags.Items.MUSHROOMS)
	}

	override fun aiStep() {
		val previousBlockPositions = getOccupiedBlockPositions().toSet()
		super.aiStep()

		val newBlockPositions = getOccupiedBlockPositions().toMutableList()
		newBlockPositions.removeAll(previousBlockPositions)

		for (newBlockPosition in newBlockPositions) {
			tryApplyBonemeal(newBlockPosition)
		}

		if (isServerSide && isAlive && !isBaby) {
			tryDropSlime()
		}
	}

	private fun tryDropSlime() {
		slimeDropTime--
		if (slimeDropTime > 0) return

		playSound(
			SoundEvents.CHICKEN_EGG,
			1f,
			random.nextRange(0.8f, 1.2f)
		)

		spawnAtLocation(ModItems.SNAIL_SLIME.get())
		gameEvent(GameEvent.ENTITY_PLACE)

		slimeDropTime = random.nextRange(6000, 12001)
	}

	private fun getOccupiedBlockPositions(): List<BlockPos> {
		return BlockPos.betweenClosedStream(boundingBox)
			.map(BlockPos::immutable)
			.toList()
	}

	private fun tryApplyBonemeal(position: BlockPos) {
		val level = level() as? ServerLevel ?: return
		val state = level.getBlockState(position)
		val block = state.block

		if (block is BonemealableBlock
			&& block.isValidBonemealTarget(level, position, state)
			&& random.chance(ServerConfig.CONFIG.sproutsnailBonemealChance.get())
		) {
			block.performBonemeal(level, random, position, state)
			level.levelEvent(LevelEvent.PARTICLES_AND_SOUND_PLANT_GROWTH, position, 0)
		}
	}

	override fun getBreedOffspring(level: ServerLevel, otherParent: AgeableMob): AgeableMob? {
		return null
	}

	override fun readAdditionalSaveData(compound: CompoundTag) {
		super.readAdditionalSaveData(compound)
		slimeDropTime = compound.getInt(SLIME_DROP_TIME_TAG)
	}

	override fun addAdditionalSaveData(compound: CompoundTag) {
		super.addAdditionalSaveData(compound)
		compound.putInt(SLIME_DROP_TIME_TAG, slimeDropTime)
	}

	override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar) {}
	override fun getAnimatableInstanceCache(): AnimatableInstanceCache = animatableInstanceCache

	companion object {
		const val WIDTH = 0.6f
		const val HEIGHT = 0.7f
		private const val SLIME_DROP_TIME_TAG = "SlimeDropTime"

		fun createAttributes(): AttributeSupplier {
			return createMobAttributes()
				.add(Attributes.MAX_HEALTH, 10.0)
				.add(Attributes.MOVEMENT_SPEED, 0.15)
				.build()
		}

		fun handleDeath(event: LivingDeathEvent) {
			val sproutsnail = event.entity as? SproutsnailEntity ?: return
			val player = event.source.entity as? ServerPlayer ?: return
			val handler = EvilSnailHandler.get(player.serverLevel())

			if (handler.hasSnail(player)) return
			if (!sproutsnail.random.chance(ServerConfig.CONFIG.sproutsnailEvilChance.get())) return

			handler.addSnail(player, sproutsnail.position())
		}
	}

}