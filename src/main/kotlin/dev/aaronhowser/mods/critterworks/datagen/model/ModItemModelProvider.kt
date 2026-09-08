package dev.aaronhowser.mods.critterworks.datagen.model

import dev.aaronhowser.mods.critterworks.Critterworks
import dev.aaronhowser.mods.critterworks.registry.ModItems
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.data.PackOutput
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemDisplayContext
import net.neoforged.neoforge.client.model.generators.ItemModelProvider
import net.neoforged.neoforge.client.model.generators.ModelFile
import net.neoforged.neoforge.common.data.ExistingFileHelper

class ModItemModelProvider(
	output: PackOutput,
	existingFileHelper: ExistingFileHelper
) : ItemModelProvider(output, Critterworks.MOD_ID, existingFileHelper) {

	override fun registerModels() {
		basicItems()
		bewlrs()
		dyeberryItems()
		hoppingSpider()

		spawnEggItem(ModItems.SCOOCHWORM_SPAWN_EGG.get())
	}

	private fun hoppingSpider() {
		val hoppingSpiderModel = getBuilder("hopping_spider")
			.parent(ModelFile.UncheckedModelFile(mcLoc("builtin/entity")))
			.texture("particle", modLoc("item/hopping_spider"))

		hoppingSpiderModel.transforms()
			.transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)
			.rotation(64f, 0f, 0f)
			.translation(-0.1f, 1f, -0.75f)
			.scale(0.4f)
			.end()
			.transform(ItemDisplayContext.THIRD_PERSON_LEFT_HAND)
			.rotation(64f, 0f, 0f)
			.translation(0.1f, 0f, -0.75f)
			.scale(0.4f)
			.end()
			.transform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
			.rotation(-180f, 65f, -180f)
			.scale(0.5f)
			.end()
			.transform(ItemDisplayContext.FIRST_PERSON_LEFT_HAND)
			.rotation(-180f, 65f, 180f)
			.scale(0.5f)
			.end()
			.transform(ItemDisplayContext.GROUND)
			.scale(0.5f)
			.end()
			.transform(ItemDisplayContext.GUI)
			.rotation(-152.25f, 29.25f, -180f)
			.translation(0f, -4f, 0f)
			.end()
			.transform(ItemDisplayContext.HEAD)
			.rotation(90f, 0f, -180f)
			.translation(0f, 0f, -4.5f)
			.end()
			.transform(ItemDisplayContext.FIXED)
			.rotation(90f, 0f, -180f)
			.end()
	}

	private fun bewlrs() {
		val items = listOf(
			ModItems.SCOOCHWORM_GPS
		)

		for (item in items) {
			val id = item.id.path

			getBuilder(id)
				.parent(ModelFile.UncheckedModelFile("builtin/entity"))
		}
	}

	private fun basicItems() {
		basicItem(ModItems.LOCKBOX.get())
		basicItem(ModItems.ARTIFICIAL_SPINNERETS.get())
		basicItem(ModItems.WEB_PATHFINDER.get())
		basicItem(ModItems.ITEM_FILTER.get())
		basicItem(ModItems.WEB_PORT.get())
	}

	private fun dyeberryItems() {
		fun dyeberryItem(item: Item, textureName: String) {
			val itemName = BuiltInRegistries.ITEM.getKey(item).path

			withExistingParent(itemName, mcLoc("item/generated"))
				.texture("layer0", modLoc("item/dyeberry/$textureName"))
		}

		dyeberryItem(ModItems.GREEN_DYEBERRY.get(), "green")
		dyeberryItem(ModItems.BLUE_DYEBERRY.get(), "blue")
		dyeberryItem(ModItems.RED_DYEBERRY.get(), "red")
		dyeberryItem(ModItems.YELLOW_DYEBERRY.get(), "yellow")
		dyeberryItem(ModItems.MAGENTA_DYEBERRY.get(), "magenta")
		dyeberryItem(ModItems.CYAN_DYEBERRY.get(), "cyan")
		dyeberryItem(ModItems.AARONBERRY.get(), "aaron")
	}

}