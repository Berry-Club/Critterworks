package dev.aaronhowser.mods.critterworks.block_entity

import dev.aaronhowser.mods.aaron.block_entity.SyncingBlockEntity
import dev.aaronhowser.mods.critterworks.block.CritterCageBlock
import dev.aaronhowser.mods.critterworks.entity.scoochworm.ScoochwormEntity
import dev.aaronhowser.mods.critterworks.item.CritterCageItem
import dev.aaronhowser.mods.critterworks.registry.ModBlockEntityTypes
import dev.aaronhowser.mods.critterworks.registry.ModDataComponents
import dev.aaronhowser.mods.critterworks.registry.ModEntityTypes
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.core.component.DataComponentMap
import net.minecraft.nbt.CompoundTag
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
	private var cachedScoochworm: ScoochwormEntity? = null

	var storedEntityData: CustomData? = null
		set(value) {
			field = value
			cachedEntityData = null
			cachedEntityLevel = null
			cachedScoochworm = null
			setChanged()
		}

	val hasEntity: Boolean
		get() = storedEntityData != null

	fun getScoochworm(): ScoochwormEntity? {
		val data = storedEntityData ?: return null
		val level = level ?: return null

		if (data == cachedEntityData && level === cachedEntityLevel) {
			return cachedScoochworm
		}

		val scoochworm = ScoochwormEntity(ModEntityTypes.SCOOCHWORM.get(), level)
		scoochworm.load(data.copyTag())

		cachedEntityData = data
		cachedEntityLevel = level
		cachedScoochworm = scoochworm

		return scoochworm
	}

	fun tryCapture(scoochworm: ScoochwormEntity): Boolean {
		val level = level ?: return false
		if (level.isClientSide || hasEntity || !blockState.getValue(CritterCageBlock.OPEN)) return false

		storedEntityData = CritterCageItem.createEntityData(scoochworm)
		level.setBlock(
			blockPos,
			blockState.setValue(CritterCageBlock.OPEN, false),
			Block.UPDATE_CLIENTS
		)
		scoochworm.discard()

		return true
	}

	fun tryRelease(): Boolean {
		val level = level ?: return false
		val data = storedEntityData ?: return false
		val forward = blockState.getValue(CritterCageBlock.FORWARD)
		val spawnPos = blockPos.relative(forward)

		val stack = ItemStack(blockState.block)
		stack.set(ModDataComponents.ENTITY_DATA, data)

		val worm = CritterCageItem.placeScoochworm(
			stack,
			level,
			spawnPos,
			forward
		) ?: return false

		if (!level.noCollision(worm)) {
			worm.discard()
			return false
		}

		worm.isTryingToMove = true

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