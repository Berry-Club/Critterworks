package dev.aaronhowser.mods.critterworks.entity.filterfeeder

import dev.aaronhowser.mods.aaron.misc.AaronExtensions.chance
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.getDefaultInstance
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isFluid
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isServerSide
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.toComponent
import dev.aaronhowser.mods.critterworks.config.ServerConfig
import dev.aaronhowser.mods.critterworks.datagen.language.ModEntityLang
import dev.aaronhowser.mods.critterworks.entity.filterfeeder.control.FilterfeederMoveControl
import dev.aaronhowser.mods.critterworks.entity.filterfeeder.goal.FilterfeederSwimGoal
import dev.aaronhowser.mods.critterworks.item.FilterfeederItem
import dev.aaronhowser.mods.critterworks.registry.ModItems
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.tags.FluidTags
import net.minecraft.util.RandomSource
import net.minecraft.world.DifficultyInstance
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MobSpawnType
import net.minecraft.world.entity.SpawnGroupData
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.animal.AbstractFish
import net.minecraft.world.entity.animal.AbstractSchoolingFish
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.ServerLevelAccessor
import net.minecraft.world.phys.Vec3
import software.bernie.geckolib.animatable.GeoEntity
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.animatable.instance.SingletonAnimatableInstanceCache
import software.bernie.geckolib.animation.*

class FilterfeederEntity(
	entityType: EntityType<out FilterfeederEntity>,
	level: Level
) : AbstractSchoolingFish(entityType, level), GeoEntity {

	init {
		moveControl = FilterfeederMoveControl(this)
	}

	var filterType: FilterType
		get() = FilterType.fromOrdinal(entityData.get(DATA_FILTER_TYPE))
		private set(value) = entityData.set(DATA_FILTER_TYPE, value.ordinal)

	private var filterStack = ItemStack.EMPTY

	private val animatableInstanceCache = SingletonAnimatableInstanceCache(this)

	protected override fun getTypeName(): Component {
		return filterType.translationKey.toComponent()
	}

	override fun getBucketItemStack(): ItemStack = ItemStack.EMPTY

	protected override fun mobInteract(player: Player, hand: InteractionHand): InteractionResult {
		if (level().isClientSide) return InteractionResult.SUCCESS

		val returnedStack = getFilterStack().copy()
		if (!player.addItem(returnedStack)) {
			player.drop(returnedStack, false)
		}

		discard()
		return InteractionResult.CONSUME
	}

	fun setFilterStack(stack: ItemStack) {
		filterStack = stack.copy()

		val filterItem = stack.item
		if (filterItem is FilterfeederItem) {
			filterType = filterItem.filterType
		}
	}

	private fun getFilterStack(): ItemStack {
		if (!filterStack.isEmpty) return filterStack

		return when (filterType) {
			FilterType.ITEM -> ModItems.ITEM_FILTERFEEDER.getDefaultInstance()
			FilterType.FLUID -> ModItems.FLUID_FILTERFEEDER.getDefaultInstance()
		}
	}

	protected override fun getFlopSound(): SoundEvent = SoundEvents.COD_FLOP

	override fun registerGoals() {
		super.registerGoals()
		goalSelector.addGoal(4, FilterfeederSwimGoal(this))
	}

	protected override fun canRandomSwim(): Boolean = false

	fun isWithinFloorRange(
		position: BlockPos = blockPosition(),
		maximumDistance: Int = MAXIMUM_FLOOR_DISTANCE
	): Boolean {
		val floorPosition = position.mutable()
		var distance = 0

		while (distance <= maximumDistance) {
			val floorState = level().getBlockState(floorPosition)
			if (floorState.isFaceSturdy(level(), floorPosition, Direction.UP)) return true

			floorPosition.move(Direction.DOWN)
			distance++
		}

		return false
	}

	override fun finalizeSpawn(
		level: ServerLevelAccessor,
		difficulty: DifficultyInstance,
		spawnType: MobSpawnType,
		spawnGroupData: SpawnGroupData?
	): SpawnGroupData? {
		val finalizedSpawnGroupData = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData)

		if (random.chance(FLUID_SPAWN_CHANCE)) {
			filterType = FilterType.FLUID
		}

		return finalizedSpawnGroupData
	}

	override fun defineSynchedData(builder: SynchedEntityData.Builder) {
		super.defineSynchedData(builder)
		builder.define(DATA_FILTER_TYPE, FilterType.ITEM.ordinal)
	}

	override fun readAdditionalSaveData(tag: CompoundTag) {
		super.readAdditionalSaveData(tag)
		filterType = FilterType.fromOrdinal(tag.getInt(FILTER_TYPE_TAG))

		if (tag.contains(FILTER_STACK_TAG)) {
			val savedStack = ItemStack.parseOptional(
				registryAccess(),
				tag.getCompound(FILTER_STACK_TAG)
			)

			setFilterStack(savedStack)
		}
	}

	override fun addAdditionalSaveData(tag: CompoundTag) {
		super.addAdditionalSaveData(tag)
		tag.putInt(FILTER_TYPE_TAG, filterType.ordinal)

		if (!filterStack.isEmpty) {
			tag.put(
				FILTER_STACK_TAG,
				filterStack.save(registryAccess())
			)
		}
	}

	override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar) {
		controllers.add(AnimationController(this, "main", 0, ::selectAnimation))
	}

	private fun selectAnimation(animationState: AnimationState<FilterfeederEntity>): PlayState {
		if (!animationState.isMoving) return PlayState.STOP

		return animationState.setAndContinue(SWIM_ANIMATION)
	}

	override fun getAnimatableInstanceCache(): AnimatableInstanceCache = animatableInstanceCache

	companion object {
		const val WIDTH = 0.5f
		const val HEIGHT = 0.125f
		const val MAXIMUM_FLOOR_DISTANCE = 10
		private const val FLUID_SPAWN_CHANCE = 0.2f

		private const val FILTER_TYPE_TAG = "FilterType"
		private const val FILTER_STACK_TAG = "FilterStack"

		private val SWIM_ANIMATION: RawAnimation? =
			RawAnimation.begin()
				.thenLoop("animation.clam.swim")

		private val DATA_FILTER_TYPE: EntityDataAccessor<Int> =
			SynchedEntityData.defineId(
				FilterfeederEntity::class.java,
				EntityDataSerializers.INT
			)

		fun createAttributes(): AttributeSupplier {
			return AbstractFish.createAttributes()
				.add(Attributes.ARMOR, 4.0)
				.build()
		}

		fun checkSpawnRules(
			entityType: EntityType<FilterfeederEntity>,
			level: ServerLevelAccessor,
			spawnType: MobSpawnType,
			position: BlockPos,
			random: RandomSource
		): Boolean {
			val floorPosition = position.below()

			return level.getFluidState(position).isFluid(FluidTags.WATER)
				&& level.getBlockState(floorPosition).isFaceSturdy(level, floorPosition, Direction.UP)
		}

		@JvmStatic
		fun getMinWaterFog(level: Level, position: Vec3): Float {
			if (level.isServerSide) return 0f
			if (level !is ClientLevel) return 0f

			val radius = ServerConfig.CONFIG.filterfeederWaterFogRadius.get()
			if (radius <= 0.0) return 0f

			val radiusSquared = radius * radius
			var nearestDistanceSquared = Double.POSITIVE_INFINITY

			for (entity in level.entitiesForRendering()) {
				if (entity !is FilterfeederEntity) continue

				val distanceSquared = entity.position().distanceToSqr(position)
				if (distanceSquared >= nearestDistanceSquared) continue

				nearestDistanceSquared = distanceSquared
			}

			if (nearestDistanceSquared >= radiusSquared) return 0f

			val normalizedDistanceSquared = nearestDistanceSquared / radiusSquared
			val fogStrength = 1.0 - normalizedDistanceSquared * normalizedDistanceSquared

			return fogStrength.toFloat()
		}
	}

	enum class FilterType(
		val translationKey: String
	) {
		ITEM(ModEntityLang.ITEM_FILTERFEEDER),
		FLUID(ModEntityLang.FLUID_FILTERFEEDER);

		companion object {
			fun fromOrdinal(ordinal: Int): FilterType {
				return entries.getOrNull(ordinal) ?: ITEM
			}
		}
	}

}