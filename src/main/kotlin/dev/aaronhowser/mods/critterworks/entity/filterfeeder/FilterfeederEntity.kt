package dev.aaronhowser.mods.critterworks.entity.filterfeeder

import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isFluid
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.toComponent
import dev.aaronhowser.mods.critterworks.datagen.language.ModEntityLang
import dev.aaronhowser.mods.critterworks.entity.filterfeeder.control.FilterfeederMoveControl
import dev.aaronhowser.mods.critterworks.entity.filterfeeder.goal.FilterfeederSwimGoal
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
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MobSpawnType
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.animal.AbstractFish
import net.minecraft.world.entity.animal.AbstractSchoolingFish
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.ServerLevelAccessor
import software.bernie.geckolib.animatable.GeoEntity
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.animatable.instance.SingletonAnimatableInstanceCache
import software.bernie.geckolib.animation.AnimatableManager

class FilterfeederEntity(
	entityType: EntityType<out FilterfeederEntity>,
	level: Level
) : AbstractSchoolingFish(entityType, level), GeoEntity {

	init {
		moveControl = FilterfeederMoveControl(this)
	}

	var filterType: FilterType
		get() = FilterType.fromOrdinal(entityData.get(DATA_FILTER_TYPE))
		set(value) = entityData.set(DATA_FILTER_TYPE, value.ordinal)

	private val animatableInstanceCache = SingletonAnimatableInstanceCache(this)

	protected override fun getTypeName(): Component {
		return filterType.translationKey.toComponent()
	}

	override fun getBucketItemStack(): ItemStack = ItemStack.EMPTY

	protected override fun mobInteract(player: Player, hand: InteractionHand): InteractionResult {
		return InteractionResult.PASS
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

	override fun defineSynchedData(builder: SynchedEntityData.Builder) {
		super.defineSynchedData(builder)
		builder.define(DATA_FILTER_TYPE, FilterType.ITEM.ordinal)
	}

	override fun readAdditionalSaveData(tag: CompoundTag) {
		super.readAdditionalSaveData(tag)
		filterType = FilterType.fromOrdinal(tag.getInt(FILTER_TYPE_TAG))
	}

	override fun addAdditionalSaveData(tag: CompoundTag) {
		super.addAdditionalSaveData(tag)
		tag.putInt(FILTER_TYPE_TAG, filterType.ordinal)
	}

	override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar) = Unit
	override fun getAnimatableInstanceCache(): AnimatableInstanceCache = animatableInstanceCache

	companion object {
		const val WIDTH = 0.5f
		const val HEIGHT = 0.125f
		const val MAXIMUM_FLOOR_DISTANCE = 10
		private const val FILTER_TYPE_TAG = "FilterType"

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