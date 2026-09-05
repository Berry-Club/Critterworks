package dev.aaronhowser.mods.critterworks.registry

import dev.aaronhowser.mods.aaron.registry.AaronItemRegistry
import dev.aaronhowser.mods.critterworks.Critterworks
import dev.aaronhowser.mods.critterworks.entity.data.WormColor
import dev.aaronhowser.mods.critterworks.item.*
import net.neoforged.neoforge.registries.DeferredItem
import net.neoforged.neoforge.registries.DeferredRegister

object ModItems : AaronItemRegistry() {

	val ITEM_REGISTRY: DeferredRegister.Items = DeferredRegister.createItems(Critterworks.MOD_ID)
	override fun getItemRegistry(): DeferredRegister.Items = ITEM_REGISTRY

	val LOCKBOX: DeferredItem<LockboxItem> =
		register("lockbox", ::LockboxItem, LockboxItem.DEFAULT_PROPERTIES)
	val ARTIFICIAL_SPINNERETS: DeferredItem<ArtificialSpinneretsItem> =
		register("artificial_spinnerets", ::ArtificialSpinneretsItem, ArtificialSpinneretsItem.DEFAULT_PROPERTIES)
	val WEB_PATHFINDER: DeferredItem<WebPathfinderItem> =
		register("web_pathfinder", ::WebPathfinderItem, PROPERTIES_SINGLE_STACK)
	val ITEM_FILTER: DeferredItem<ItemFilterItem> =
		register("item_filter", ::ItemFilterItem, PROPERTIES_SINGLE_STACK)
	val WEB_PORT: DeferredItem<WebPortItem> =
		register("web_port", ::WebPortItem, PROPERTIES_SINGLE_STACK)
	val HOPPING_SPIDER: DeferredItem<HoppingSpiderItem> =
		register("hopping_spider", ::HoppingSpiderItem)
	val CRITTER_CAGE: DeferredItem<CritterCageItem> =
		register("critter_cage", ::CritterCageItem)
	val SCOOCHWORM_SPAWN_EGG: DeferredItem<ScoochwormSpawnEggItem> =
		register("scoochworm_spawn_egg", ::ScoochwormSpawnEggItem)

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

}