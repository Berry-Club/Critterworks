package dev.aaronhowser.mods.critterworks.entity.sproutsnail

import dev.aaronhowser.mods.aaron.misc.AaronExtensions.asIngredient
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.chance
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.giveOrDropStack
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isClientSide
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isHolder
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isItem
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isSameItemSameComponents
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isServerSide
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.nextRange
import dev.aaronhowser.mods.critterworks.advancement.ModAdvancements
import dev.aaronhowser.mods.critterworks.config.ServerConfig
import dev.aaronhowser.mods.critterworks.datagen.tag.ModItemTagsProvider
import dev.aaronhowser.mods.critterworks.entity.sproutsnail.goal.SproutsnailMoveToBonemealableBlockGoal
import dev.aaronhowser.mods.critterworks.handler.snail.EvilSnailHandler
import dev.aaronhowser.mods.critterworks.registry.ModItems
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.Registries
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.DifficultyInstance
import net.minecraft.world.entity.AgeableMob
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MobSpawnType
import net.minecraft.world.entity.SpawnGroupData
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.ai.goal.FloatGoal
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal
import net.minecraft.world.entity.ai.goal.PanicGoal
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal
import net.minecraft.world.entity.ai.goal.RandomStrollGoal
import net.minecraft.world.entity.ai.goal.TemptGoal
import net.minecraft.world.entity.ai.targeting.TargetingConditions
import net.minecraft.world.entity.animal.Animal
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.ServerLevelAccessor
import net.minecraft.world.level.block.BonemealableBlock
import net.minecraft.world.level.block.LevelEvent
import net.minecraft.world.level.biome.Biomes
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

	var flower: ItemStack
		get() = entityData.get(DATA_FLOWER)
		private set(value) {
			val storedFlower = if (value.isEmpty) ItemStack.EMPTY else value.copyWithCount(1)
			entityData.set(DATA_FLOWER, storedFlower)
		}

	override fun registerGoals() {
		goalSelector.addGoal(0, FloatGoal(this))
		goalSelector.addGoal(1, PanicGoal(this, 1.25))
		goalSelector.addGoal(2, TemptGoal(this, 1.0, Tags.Items.MUSHROOMS.asIngredient(), false))
		goalSelector.addGoal(3, SproutsnailMoveToBonemealableBlockGoal(this))
		goalSelector.addGoal(4, RandomStrollGoal(this, 1.0))
		goalSelector.addGoal(5, LookAtPlayerGoal(this, Player::class.java, 6f))
		goalSelector.addGoal(6, RandomLookAroundGoal(this))
	}

	override fun isFood(stack: ItemStack): Boolean {
		return stack.isItem(Tags.Items.MUSHROOMS)
	}

	override fun finalizeSpawn(
		level: ServerLevelAccessor,
		difficulty: DifficultyInstance,
		spawnType: MobSpawnType,
		spawnGroupData: SpawnGroupData?
	): SpawnGroupData? {
		val finalizedSpawnGroupData = super.finalizeSpawn(
			level,
			difficulty,
			spawnType,
			spawnGroupData
		)

		val currentBiome = level.getBiome(blockPosition())
		val spawnPlants = when {
			currentBiome.isHolder(Tags.Biomes.IS_NETHER) ->
				ModItemTagsProvider.SPROUTSNAIL_SPAWN_PLANTS_NETHER

			currentBiome.isHolder(Tags.Biomes.IS_MUSHROOM) ->
				ModItemTagsProvider.SPROUTSNAIL_SPAWN_PLANTS_MUSHROOM

			currentBiome.isHolder(Biomes.LUSH_CAVES) ->
				ModItemTagsProvider.SPROUTSNAIL_SPAWN_PLANTS_LUSH_CAVES

			currentBiome.isHolder(Tags.Biomes.IS_FLORAL) ->
				ModItemTagsProvider.SPROUTSNAIL_SPAWN_PLANTS_FLORAL

			currentBiome.isHolder(Tags.Biomes.IS_SWAMP) ->
				ModItemTagsProvider.SPROUTSNAIL_SPAWN_PLANTS_SWAMP

			currentBiome.isHolder(Tags.Biomes.IS_JUNGLE) ->
				ModItemTagsProvider.SPROUTSNAIL_SPAWN_PLANTS_JUNGLE

			currentBiome.isHolder(Tags.Biomes.IS_DESERT)
				|| currentBiome.isHolder(Tags.Biomes.IS_BADLANDS) ->
				ModItemTagsProvider.SPROUTSNAIL_SPAWN_PLANTS_ARID

			currentBiome.isHolder(Tags.Biomes.IS_SAVANNA) ->
				ModItemTagsProvider.SPROUTSNAIL_SPAWN_PLANTS_SAVANNA

			currentBiome.isHolder(Tags.Biomes.IS_TAIGA) ->
				ModItemTagsProvider.SPROUTSNAIL_SPAWN_PLANTS_TAIGA

			currentBiome.isHolder(Tags.Biomes.IS_FOREST) ->
				ModItemTagsProvider.SPROUTSNAIL_SPAWN_PLANTS_FOREST

			currentBiome.isHolder(Tags.Biomes.IS_PLAINS) ->
				ModItemTagsProvider.SPROUTSNAIL_SPAWN_PLANTS_PLAINS

			else -> ModItemTagsProvider.SPROUTSNAIL_SPAWN_PLANTS_DEFAULT
		}

		val itemRegistry = level.registryAccess()
			.registryOrThrow(Registries.ITEM)

		val randomPlant = itemRegistry
			.getRandomElementOf(spawnPlants, random)
			.orElse(null)

		if (randomPlant != null) {
			flower = ItemStack(randomPlant.value())
		}

		return finalizedSpawnGroupData
	}

	override fun mobInteract(player: Player, hand: InteractionHand): InteractionResult {
		val heldStack = player.getItemInHand(hand)

		if (heldStack.isItem(ModItemTagsProvider.SPROUTSNAIL_FLOWERS)) {
			if (flower.isSameItemSameComponents(heldStack)) return InteractionResult.PASS

			if (isServerSide) {
				val previousFlower = flower.copy()
				flower = heldStack
				heldStack.consume(1, player)
				player.giveOrDropStack(previousFlower)

				setPersistenceRequired()
				playSound(SoundEvents.GRASS_PLACE, 1f, 1f)
				gameEvent(GameEvent.BLOCK_CHANGE, player)
			}

			return InteractionResult.sidedSuccess(isClientSide)
		} else if (heldStack.isEmpty && !flower.isEmpty) {
			if (isServerSide) {
				player.giveOrDropStack(flower.copy())
				flower = ItemStack.EMPTY

				playSound(SoundEvents.ITEM_FRAME_REMOVE_ITEM, 1f, 1f)
				gameEvent(GameEvent.BLOCK_CHANGE, player)
			}

			return InteractionResult.sidedSuccess(isClientSide)
		}

		return super.mobInteract(player, hand)
	}

	override fun tick() {
		super.tick()

		if (isServerSide && tickCount % 200 == 0) {
			val nearbyPlayers = level().getNearbyPlayers(TargetingConditions.forNonCombat(), this, boundingBox.inflate(20.0))
			for (player in nearbyPlayers) {
				ModAdvancements.trigger(player, ModAdvancements.FIND_SPROUTSNAIL)
			}
		}
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

			val nearbyPlayers = level.getNearbyPlayers(TargetingConditions.forNonCombat(), this, boundingBox.inflate(20.0))
			for (player in nearbyPlayers) {
				ModAdvancements.trigger(player, ModAdvancements.USE_SPROUTSNAIL)
			}
		}
	}

	override fun dropEquipment() {
		super.dropEquipment()

		if (flower.isEmpty) return

		spawnAtLocation(flower.copy())
		flower = ItemStack.EMPTY
	}

	override fun getBreedOffspring(level: ServerLevel, otherParent: AgeableMob): AgeableMob? {
		return null
	}

	override fun readAdditionalSaveData(compound: CompoundTag) {
		super.readAdditionalSaveData(compound)

		if (compound.contains(SLIME_DROP_TIME_TAG)) {
			slimeDropTime = compound.getInt(SLIME_DROP_TIME_TAG)
		}

		if (compound.contains(FLOWER_TAG)) {
			flower = ItemStack.parseOptional(
				registryAccess(),
				compound.getCompound(FLOWER_TAG)
			)
		}
	}

	override fun addAdditionalSaveData(compound: CompoundTag) {
		super.addAdditionalSaveData(compound)
		compound.putInt(SLIME_DROP_TIME_TAG, slimeDropTime)

		if (!flower.isEmpty) {
			compound.put(
				FLOWER_TAG,
				flower.save(registryAccess())
			)
		}
	}

	override fun defineSynchedData(builder: SynchedEntityData.Builder) {
		super.defineSynchedData(builder)
		builder.define(DATA_FLOWER, ItemStack.EMPTY)
	}

	override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar) {}
	override fun getAnimatableInstanceCache(): AnimatableInstanceCache = animatableInstanceCache

	companion object {
		const val WIDTH = 0.6f
		const val HEIGHT = 0.7f
		private const val SLIME_DROP_TIME_TAG = "SlimeDropTime"
		private const val FLOWER_TAG = "Flower"

		private val DATA_FLOWER: EntityDataAccessor<ItemStack> =
			SynchedEntityData.defineId(
				SproutsnailEntity::class.java,
				EntityDataSerializers.ITEM_STACK
			)

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