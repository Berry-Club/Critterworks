package dev.aaronhowser.mods.critterworks.registry

import dev.aaronhowser.mods.aaron.registry.AaronDataComponentRegistry
import dev.aaronhowser.mods.critterworks.Critterworks
import dev.aaronhowser.mods.critterworks.entity.data.WormColor
import dev.aaronhowser.mods.critterworks.item.component.ItemFilterComponent
import dev.aaronhowser.mods.critterworks.item.component.WebNodeDataComponent
import dev.aaronhowser.mods.critterworks.item.component.WebPortComponent
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.registries.Registries
import net.minecraft.world.item.component.CustomData
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister

object ModDataComponents : AaronDataComponentRegistry() {

	val DATA_COMPONENT_REGISTRY: DeferredRegister.DataComponents =
		DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Critterworks.MOD_ID)

	override fun getDataComponentRegistry(): DeferredRegister.DataComponents = DATA_COMPONENT_REGISTRY

	val ENTITY_DATA: DeferredHolder<DataComponentType<*>, DataComponentType<CustomData>> =
		register("entity_data", CustomData.CODEC, CustomData.STREAM_CODEC)
	val WORM_COLOR: DeferredHolder<DataComponentType<*>, DataComponentType<WormColor>> =
		register("worm_color", WormColor.CODEC, WormColor.STREAM_CODEC)
	val WEB_NODE: DeferredHolder<DataComponentType<*>, DataComponentType<WebNodeDataComponent>> =
		register("web_node", WebNodeDataComponent.CODEC, WebNodeDataComponent.STREAM_CODEC)
	val ITEM_FILTER: DeferredHolder<DataComponentType<*>, DataComponentType<ItemFilterComponent>> =
		register("item_filter", ItemFilterComponent.CODEC, ItemFilterComponent.STREAM_CODEC)
	val WEB_PORT: DeferredHolder<DataComponentType<*>, DataComponentType<WebPortComponent>> =
		register("web_port", WebPortComponent.CODEC, WebPortComponent.STREAM_CODEC)
	val WEB_FLUID: DeferredHolder<DataComponentType<*>, DataComponentType<Double>> =
		double("web_fluid")

}