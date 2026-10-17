package dev.aaronhowser.mods.critterworks.entity.filterfeeder

import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isFluid
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.toComponent
import dev.aaronhowser.mods.critterworks.datagen.language.ModEntityLang
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.tags.FluidTags
import net.minecraft.util.RandomSource
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MobSpawnType
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.animal.WaterAnimal
import net.minecraft.world.level.Level
import net.minecraft.world.level.ServerLevelAccessor
import software.bernie.geckolib.animatable.GeoEntity
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.animatable.instance.SingletonAnimatableInstanceCache
import software.bernie.geckolib.animation.AnimatableManager

class FilterfeederEntity(
	entityType: EntityType<out FilterfeederEntity>,
	level: Level
) : WaterAnimal(entityType, level), GeoEntity {

	var filterType: FilterType
		get() = FilterType.fromOrdinal(entityData.get(DATA_FILTER_TYPE))
		set(value) = entityData.set(DATA_FILTER_TYPE, value.ordinal)

	private val animatableInstanceCache = SingletonAnimatableInstanceCache(this)

	protected override fun getTypeName(): Component {
		return filterType.translationKey.toComponent()
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
		private const val FILTER_TYPE_TAG = "FilterType"

		private val DATA_FILTER_TYPE: EntityDataAccessor<Int> =
			SynchedEntityData.defineId(
				FilterfeederEntity::class.java,
				EntityDataSerializers.INT
			)

		fun createAttributes(): AttributeSupplier {
			return createMobAttributes()
				.add(Attributes.MAX_HEALTH, 4.0)
				.add(Attributes.MOVEMENT_SPEED, 0.1)
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