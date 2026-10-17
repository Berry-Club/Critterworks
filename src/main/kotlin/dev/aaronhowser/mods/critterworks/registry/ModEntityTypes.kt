package dev.aaronhowser.mods.critterworks.registry

import dev.aaronhowser.mods.critterworks.Critterworks
import dev.aaronhowser.mods.critterworks.entity.filterfeeder.FilterfeederEntity
import dev.aaronhowser.mods.critterworks.entity.scoochworm.ScoochwormEntity
import dev.aaronhowser.mods.critterworks.entity.scoochworm.ScoochwormPartEntity
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MobCategory
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister
import java.util.function.Supplier

object ModEntityTypes {

	val ENTITY_TYPE_REGISTRY: DeferredRegister<EntityType<*>> =
		DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, Critterworks.MOD_ID)

	val FILTERFEEDER: DeferredHolder<EntityType<*>, EntityType<FilterfeederEntity>> =
		ENTITY_TYPE_REGISTRY.register("filterfeeder", Supplier {
			EntityType.Builder.of(::FilterfeederEntity, MobCategory.WATER_CREATURE)
				.sized(FilterfeederEntity.WIDTH, FilterfeederEntity.HEIGHT)
				.build("filterfeeder")
		})

	val SCOOCHWORM: DeferredHolder<EntityType<*>, EntityType<ScoochwormEntity>> =
		ENTITY_TYPE_REGISTRY.register("scoochworm", Supplier {
			EntityType.Builder.of(::ScoochwormEntity, MobCategory.CREATURE)
				.sized(ScoochwormEntity.SIZE, ScoochwormEntity.SIZE)
				.build("scoochworm")
		})

	val SCOOCHWORM_PART: DeferredHolder<EntityType<*>, EntityType<ScoochwormPartEntity>> =
		ENTITY_TYPE_REGISTRY.register("scoochworm_part", Supplier {
			EntityType.Builder.of(::ScoochwormPartEntity, MobCategory.MISC)
				.sized(ScoochwormEntity.SIZE, ScoochwormEntity.SIZE)
				.clientTrackingRange(10)
				.updateInterval(3)
				.build("scoochworm_part")
		})
}