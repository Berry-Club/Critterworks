package dev.aaronhowser.mods.critterworks.registry

import dev.aaronhowser.mods.critterworks.Critterworks
import dev.aaronhowser.mods.critterworks.entity.filterfeeder.FilterfeederEntity
import dev.aaronhowser.mods.critterworks.entity.punchprawn.PunchprawnEntity
import dev.aaronhowser.mods.critterworks.entity.scoochworm.ScoochwormEntity
import dev.aaronhowser.mods.critterworks.entity.scoochworm.ScoochwormPartEntity
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MobCategory
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister
import java.util.function.Supplier

object ModEntityTypes {

	val ENTITY_TYPE_REGISTRY: DeferredRegister<EntityType<*>> =
		DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, Critterworks.MOD_ID)

	val FILTERFEEDER: DeferredHolder<EntityType<*>, EntityType<FilterfeederEntity>> =
		register(
			"filterfeeder",
			EntityType.Builder.of(::FilterfeederEntity, MobCategory.WATER_CREATURE)
				.sized(FilterfeederEntity.WIDTH, FilterfeederEntity.HEIGHT)
		)

	val SCOOCHWORM: DeferredHolder<EntityType<*>, EntityType<ScoochwormEntity>> =
		register(
			"scoochworm",
			EntityType.Builder.of(::ScoochwormEntity, MobCategory.CREATURE)
				.sized(ScoochwormEntity.SIZE, ScoochwormEntity.SIZE)
		)

	val SCOOCHWORM_PART: DeferredHolder<EntityType<*>, EntityType<ScoochwormPartEntity>> =
		register(
			"scoochworm_part",
			EntityType.Builder.of(::ScoochwormPartEntity, MobCategory.MISC)
				.sized(ScoochwormEntity.SIZE, ScoochwormEntity.SIZE)
				.clientTrackingRange(10)
				.updateInterval(3)
		)

	val PUNCHPRAWN: DeferredHolder<EntityType<*>, EntityType<PunchprawnEntity>> =
		register(
			"punchprawn",
			EntityType.Builder.of(::PunchprawnEntity, MobCategory.WATER_CREATURE)
				.sized(PunchprawnEntity.WIDTH, PunchprawnEntity.HEIGHT)
		)

	private fun <T : Entity> register(
		name: String,
		builder: EntityType.Builder<T>
	): DeferredHolder<EntityType<*>, EntityType<T>> {
		return ENTITY_TYPE_REGISTRY.register(name, Supplier {
			builder.build(name)
		})
	}
}