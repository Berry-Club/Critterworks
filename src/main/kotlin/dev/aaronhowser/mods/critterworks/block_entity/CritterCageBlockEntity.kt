package dev.aaronhowser.mods.critterworks.block_entity

import dev.aaronhowser.mods.aaron.block_entity.SyncingBlockEntity
import dev.aaronhowser.mods.critterworks.block.CritterCageBlock
import dev.aaronhowser.mods.critterworks.entity.scoochworm.ScoochwormEntity
import dev.aaronhowser.mods.critterworks.item.CritterCageItem
import dev.aaronhowser.mods.critterworks.registry.ModBlockEntityTypes
import dev.aaronhowser.mods.critterworks.registry.ModDataComponents
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.core.component.DataComponentMap
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState

class CritterCageBlockEntity(
	pos: BlockPos,
	state: BlockState
) : SyncingBlockEntity(ModBlockEntityTypes.CRITTER_CAGE.get(), pos, state) {

	override val syncImmediately: Boolean = true

	private var cachedEntityData: CustomData? = null
	private var cachedEntityLevel: Level? = null
	private var cachedEntity: LivingEntity? = null

	var storedEntityData: CustomData? = null
		set(value) {
			field = value
			cachedEntityData = null
			cachedEntityLevel = null
			cachedEntity = null
			setChanged()
		}

	val hasEntity: Boolean
		get() = storedEntityData != null

	fun getStoredEntity(): LivingEntity? {
		val data = storedEntityData ?: return null
		val level = level ?: return null

		if (data == cachedEntityData && level === cachedEntityLevel) {
			return cachedEntity
		}

		val entity = CritterCageItem.createEntity(data, level)

		cachedEntityData = data
		cachedEntityLevel = level
		cachedEntity = entity

		return entity
	}

	fun tryCapture(entity: LivingEntity): Boolean {
		val level = level ?: return false
		if (level.isClientSide || hasEntity || !blockState.getValue(CritterCageBlock.OPEN)) return false
		if (!CritterCageItem.canCapture(entity)) return false

		storedEntityData = CritterCageItem.createEntityData(entity)
		level.setBlock(
			blockPos,
			blockState.setValue(CritterCageBlock.OPEN, false),
			Block.UPDATE_CLIENTS
		)
		entity.discard()

		return true
	}

	fun tryRelease(): Boolean {
		val level = level ?: return false
		val data = storedEntityData ?: return false
		val forward = blockState.getValue(CritterCageBlock.FORWARD)
		val spawnPos = blockPos.relative(forward)

		val stack = ItemStack(blockState.block)
		stack.set(ModDataComponents.ENTITY_DATA, data)

		val entity = CritterCageItem.placeEntity(
			stack,
			level,
			spawnPos,
			forward
		) ?: return false

		if (!level.noCollision(entity)) {
			entity.discard()
			return false
		}
		if (!level.addFreshEntity(entity)) return false

		if (entity is ScoochwormEntity) {
			entity.isTryingToMove = true
		}

		storedEntityData = null

		level.setBlock(
			blockPos,
			blockState.setValue(CritterCageBlock.OPEN, true),
			Block.UPDATE_CLIENTS
		)

		return true
	}

	override fun collectImplicitComponents(components: DataComponentMap.Builder) {
		super.collectImplicitComponents(components)

		val data = storedEntityData
		if (data != null) {
			components.set(ModDataComponents.ENTITY_DATA, data)
		}
	}

	override fun applyImplicitComponents(componentInput: DataComponentInput) {
		storedEntityData = componentInput.get(ModDataComponents.ENTITY_DATA)
	}

	override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
		super.saveAdditional(tag, registries)
		tag.putBoolean(HAS_ENTITY_TAG, hasEntity)

		val data = storedEntityData ?: return
		tag.put(ENTITY_DATA_TAG, data.copyTag())
	}

	override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
		super.loadAdditional(tag, registries)
		storedEntityData = if (tag.contains(ENTITY_DATA_TAG)) {
			CustomData.of(tag.getCompound(ENTITY_DATA_TAG))
		} else {
			null
		}
	}

	companion object {
		private const val HAS_ENTITY_TAG = "HasEntity"
		private const val ENTITY_DATA_TAG = "EntityData"
	}
}