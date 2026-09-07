package dev.aaronhowser.mods.critterworks.registry

import dev.aaronhowser.mods.aaron.registry.AaronBlockEntityTypeRegistry
import dev.aaronhowser.mods.critterworks.Critterworks
import dev.aaronhowser.mods.critterworks.block_entity.CritterCageBlockEntity
import dev.aaronhowser.mods.critterworks.block_entity.HoppingSpiderNestBlockEntity
import dev.aaronhowser.mods.critterworks.block_entity.StemEncasedComparatorBlockEntity
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.level.block.entity.BlockEntityType
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister

object ModBlockEntityTypes : AaronBlockEntityTypeRegistry() {

	val BLOCK_ENTITY_REGISTRY: DeferredRegister<BlockEntityType<*>> =
		DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, Critterworks.MOD_ID)

	override fun getBlockEntityRegistry(): DeferredRegister<BlockEntityType<*>> = BLOCK_ENTITY_REGISTRY

	val CRITTER_CAGE: DeferredHolder<BlockEntityType<*>, BlockEntityType<CritterCageBlockEntity>> =
		register("critter_cage", ::CritterCageBlockEntity, ModBlocks.CRITTER_CAGE)

	val HOPPING_SPIDER_NEST: DeferredHolder<BlockEntityType<*>, BlockEntityType<HoppingSpiderNestBlockEntity>> =
		register("hopping_spider_nest", ::HoppingSpiderNestBlockEntity, ModBlocks.HOPPING_SPIDER_NEST)

	val STEM_ENCASED_COMPARATOR: DeferredHolder<BlockEntityType<*>, BlockEntityType<StemEncasedComparatorBlockEntity>> =
		register("stem_encased_comparator", ::StemEncasedComparatorBlockEntity, ModBlocks.STEM_ENCASED_COMPARATOR)
}