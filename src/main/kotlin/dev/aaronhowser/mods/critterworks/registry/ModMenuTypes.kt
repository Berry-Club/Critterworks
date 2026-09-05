package dev.aaronhowser.mods.critterworks.registry

import dev.aaronhowser.mods.aaron.registry.AaronMenuTypesRegistry
import dev.aaronhowser.mods.critterworks.Critterworks
import dev.aaronhowser.mods.critterworks.menu.item_filter.ItemFilterMenu
import dev.aaronhowser.mods.critterworks.menu.item_filter.ItemFilterScreen
import dev.aaronhowser.mods.critterworks.menu.spider_nest.SpiderNestMenu
import dev.aaronhowser.mods.critterworks.menu.spider_nest.SpiderNestScreen
import dev.aaronhowser.mods.critterworks.menu.web_port.WebPortMenu
import dev.aaronhowser.mods.critterworks.menu.web_port.WebPortScreen
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.inventory.MenuType
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister

object ModMenuTypes : AaronMenuTypesRegistry() {

	val MENU_TYPE_REGISTRY: DeferredRegister<MenuType<*>> =
		DeferredRegister.create(BuiltInRegistries.MENU, Critterworks.MOD_ID)

	override fun getMenuTypeRegistry(): DeferredRegister<MenuType<*>> = MENU_TYPE_REGISTRY

	val ITEM_FILTER: DeferredHolder<MenuType<*>, MenuType<ItemFilterMenu>> =
		register("item_filter") { IMenuTypeExtension.create(::ItemFilterMenu) }

	val WEB_PORT: DeferredHolder<MenuType<*>, MenuType<WebPortMenu>> =
		register("web_port") { IMenuTypeExtension.create(WebPortMenu::fromNetwork) }

	val SPIDER_NEST: DeferredHolder<MenuType<*>, MenuType<SpiderNestMenu>> =
		register("spider_nest") { IMenuTypeExtension.create(SpiderNestMenu::fromNetwork) }

	override fun registerScreens(event: RegisterMenuScreensEvent) {
		event.register(ITEM_FILTER.get(), ::ItemFilterScreen)
		event.register(WEB_PORT.get(), ::WebPortScreen)
		event.register(SPIDER_NEST.get(), ::SpiderNestScreen)
	}
}