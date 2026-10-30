package dev.aaronhowser.mods.critterworks.registry

import dev.aaronhowser.mods.aaron.registry.AaronItemRegistry
import dev.aaronhowser.mods.critterworks.Critterworks
import dev.aaronhowser.mods.critterworks.entity.filterfeeder.FilterfeederEntity
import dev.aaronhowser.mods.critterworks.entity.scoochworm.data.WormColor
import dev.aaronhowser.mods.critterworks.item.*
import dev.aaronhowser.mods.critterworks.item.arachnode.DirectionalArachnodeItem
import dev.aaronhowser.mods.critterworks.item.arachnode.InvertingArachnodeItem
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.Item
import net.minecraft.world.item.SpawnEggItem
import net.minecraft.world.item.component.CustomData
import net.neoforged.neoforge.registries.DeferredItem
import net.neoforged.neoforge.registries.DeferredRegister

object ModItems : AaronItemRegistry() {

	val ITEM_REGISTRY: DeferredRegister.Items = DeferredRegister.createItems(Critterworks.MOD_ID)
	override fun getItemRegistry(): DeferredRegister.Items = ITEM_REGISTRY

	val LOCKBOX: DeferredItem<LockboxItem> =
		register("lockbox", ::LockboxItem, LockboxItem.DEFAULT_PROPERTIES)
	val SCOOCHWORM_GPS: DeferredItem<Item> =
		register("scoochworm_gps", ::Item, PROPERTIES_SINGLE_STACK)
	val CRITTER_CAGE: DeferredItem<CritterCageItem> =
		register("critter_cage", ::CritterCageItem)

	val HOPPING_SPIDER: DeferredItem<HoppingSpiderItem> =
		register("hopping_spider", ::HoppingSpiderItem)
	val WEB_PORT: DeferredItem<WebPortItem> =
		register("web_port", ::WebPortItem, PROPERTIES_SINGLE_STACK)
	val ARTIFICIAL_SPINNERETS: DeferredItem<ArtificialSpinneretsItem> =
		register("artificial_spinnerets", ::ArtificialSpinneretsItem, ArtificialSpinneretsItem.DEFAULT_PROPERTIES)
	val WEBSTONE_WEAVER: DeferredItem<WebstoneWeaverItem> =
		register("webstone_weaver", ::WebstoneWeaverItem, WebstoneWeaverItem.DEFAULT_PROPERTIES)
	val DIRECTIONAL_ARACHNODE: DeferredItem<DirectionalArachnodeItem> =
		register("directional_arachnode", ::DirectionalArachnodeItem)
	val INVERTING_ARACHNODE: DeferredItem<InvertingArachnodeItem> =
		register("inverting_arachnode", ::InvertingArachnodeItem)

	val SPARKPOLLEN_POUCH: DeferredItem<SparkpollenPouchItem> =
		register("sparkpollen_pouch", ::SparkpollenPouchItem, SparkpollenPouchItem.DEFAULT_PROPERTIES)

	val ITEM_FILTERFEEDER: DeferredItem<ItemFilterItem> =
		register("item_filterfeeder", ::ItemFilterItem, PROPERTIES_SINGLE_STACK)
	val FLUID_FILTERFEEDER: DeferredItem<FluidFilterItem> =
		register("fluid_filterfeeder", ::FluidFilterItem, PROPERTIES_SINGLE_STACK)
	val TARDIGRADE: DeferredItem<Item> =
		basic("tardigrade")
	val SNAIL_SHIELD: DeferredItem<SnailShieldItem> =
		register("snail_shield", ::SnailShieldItem, SnailShieldItem.DEFAULT_PROPERTIES)

	val SNAIL_SHELL_FRAGMENT: DeferredItem<Item> =
		basic("snail_shell_fragment")
	val SNAIL_SLIME: DeferredItem<SnailSlimeItem> =
		register("snail_slime", ::SnailSlimeItem)
	val PEARL: DeferredItem<Item> =
		basic("pearl")

	val GREEN_DYEBERRY: DeferredItem<DyeberryItem> =
		registerDyeberry(WormColor.GREEN)
	val BLUE_DYEBERRY: DeferredItem<DyeberryItem> =
		registerDyeberry(WormColor.BLUE)
	val RED_DYEBERRY: DeferredItem<DyeberryItem> =
		registerDyeberry(WormColor.RED)
	val YELLOW_DYEBERRY: DeferredItem<DyeberryItem> =
		registerDyeberry(WormColor.YELLOW)
	val MAGENTA_DYEBERRY: DeferredItem<DyeberryItem> =
		registerDyeberry(WormColor.MAGENTA)
	val CYAN_DYEBERRY: DeferredItem<DyeberryItem> =
		registerDyeberry(WormColor.CYAN)
	val AARONBERRY: DeferredItem<DyeberryItem> =
		registerDyeberry(WormColor.AARON, "aaronberry")

	val SCOOCHWORM_SPAWN_EGG: DeferredItem<ScoochwormSpawnEggItem> =
		register("scoochworm_spawn_egg", ::ScoochwormSpawnEggItem)
	val PUNCHPRAWN_SPAWN_EGG: DeferredItem<SpawnEggItem> =
		registerSpawnEgg(
			"punchprawn_spawn_egg",
			ModEntityTypes.PUNCHPRAWN,
			0x0E88BB, 0xFE0901
		)
	val SPROUTSNAIL_SPAWN_EGG: DeferredItem<SpawnEggItem> =
		registerSpawnEgg(
			"sproutsnail_spawn_egg",
			ModEntityTypes.SPROUTSNAIL,
			0x7BFFA3, 0xFFA7A4
		)
	val ITEM_FILTERFEEDER_SPAWN_EGG: DeferredItem<SpawnEggItem> =
		registerSpawnEgg(
			"item_filterfeeder_spawn_egg",
			ModEntityTypes.FILTERFEEDER,
			0xF0C3D2, 0x522635
		) { filterfeederSpawnEggProperties(FilterfeederEntity.FilterType.ITEM) }
	val FLUID_FILTERFEEDER_SPAWN_EGG: DeferredItem<SpawnEggItem> =
		registerSpawnEgg(
			"fluid_filterfeeder_spawn_egg",
			ModEntityTypes.FILTERFEEDER,
			0xC4E7EF, 0x264851
		) { filterfeederSpawnEggProperties(FilterfeederEntity.FilterType.FLUID) }

	private fun filterfeederSpawnEggProperties(filterType: FilterfeederEntity.FilterType): Item.Properties {
		val entityData = CompoundTag()
		entityData.putString("id", ModEntityTypes.FILTERFEEDER.id.toString())
		entityData.putInt("FilterType", filterType.ordinal)

		return Item.Properties()
			.component(DataComponents.ENTITY_DATA, CustomData.of(entityData))
	}

	private fun registerDyeberry(
		wormColor: WormColor,
		name: String = wormColor.colorName + "_dyeberry"
	): DeferredItem<DyeberryItem> {
		return register(
			name,
			{ properties ->
				DyeberryItem(
					ModBlocks.DYEBERRY_VINES.get(),
					wormColor,
					properties
				)
			}
		) { DyeberryItem.getProperties(wormColor) }
	}

	init {
		ITEM_REGISTRY.addAlias(Critterworks.modResource("item_filter"), ITEM_FILTERFEEDER.id)
	}

}