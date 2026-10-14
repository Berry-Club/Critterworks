package dev.aaronhowser.mods.critterworks.datagen.model

import dev.aaronhowser.mods.critterworks.Critterworks
import dev.aaronhowser.mods.critterworks.registry.ModItems
import net.minecraft.core.Direction
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
		dyeberryItems()
		hoppingSpider()
		lockbox()

		spawnEggItem(ModItems.SCOOCHWORM_SPAWN_EGG.get())
	}

	private fun lockbox() {
		val lockboxModel = getBuilder("lockbox")
			.parent(ModelFile.UncheckedModelFile(mcLoc("block/block")))
			.texture("texture", modLoc("entity/scoochworm/lockbox"))
			.texture("particle", "#texture")

		lockboxModel.transforms()
			.transform(ItemDisplayContext.GUI)
			.rotation(30f, 225f, 0f)
			.scale(1.4f)
			.end()

		lockboxModel.element()
			.from(5f, 5f, 5f)
			.to(11f, 9f, 11f)
			.face(Direction.NORTH)
			.uvs(0f, 6f, 3f, 8f)
			.texture("#texture")
			.end()
			.face(Direction.EAST)
			.uvs(6f, 0f, 9f, 2f)
			.texture("#texture")
			.end()
			.face(Direction.SOUTH)
			.uvs(6f, 2f, 9f, 4f)
			.texture("#texture")
			.end()
			.face(Direction.WEST)
			.uvs(3f, 6f, 6f, 8f)
			.texture("#texture")
			.end()
			.face(Direction.UP)
			.uvs(3f, 3f, 0f, 0f)
			.texture("#texture")
			.end()
			.face(Direction.DOWN)
			.uvs(3f, 3f, 0f, 6f)
			.texture("#texture")
			.end()
			.end()

		lockboxModel.element()
			.from(5f, 9f, 5f)
			.to(11f, 11f, 11f)
			.face(Direction.NORTH)
			.uvs(6f, 4f, 9f, 5f)
			.texture("#texture")
			.end()
			.face(Direction.EAST)
			.uvs(6f, 5f, 9f, 6f)
			.texture("#texture")
			.end()
			.face(Direction.SOUTH)
			.uvs(6f, 6f, 9f, 7f)
			.texture("#texture")
			.end()
			.face(Direction.WEST)
			.uvs(6f, 7f, 9f, 8f)
			.texture("#texture")
			.end()
			.face(Direction.UP)
			.uvs(6f, 3f, 3f, 0f)
			.texture("#texture")
			.end()
			.face(Direction.DOWN)
			.uvs(6f, 3f, 3f, 6f)
			.texture("#texture")
			.end()
			.end()

		lockboxModel.element()
			.from(7f, 8f, 4f)
			.to(9f, 10f, 5f)
			.face(Direction.NORTH)
			.uvs(0f, 8f, 1f, 9f)
			.texture("#texture")
			.end()
			.face(Direction.EAST)
			.uvs(2f, 8f, 2.5f, 9f)
			.texture("#texture")
			.end()
			.face(Direction.SOUTH)
			.uvs(1f, 8f, 2f, 9f)
			.texture("#texture")
			.end()
			.face(Direction.WEST)
			.uvs(2.5f, 8f, 3f, 9f)
			.texture("#texture")
			.end()
			.face(Direction.UP)
			.uvs(4f, 8.5f, 3f, 8f)
			.texture("#texture")
			.end()
			.face(Direction.DOWN)
			.uvs(5f, 8f, 4f, 8.5f)
			.texture("#texture")
			.end()
			.end()
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

	private fun basicItems() {
		basicItem(ModItems.ARTIFICIAL_SPINNERETS.get())
		basicItem(ModItems.WEB_PATHFINDER.get())
		basicItem(ModItems.ITEM_FILTER.get())
		basicItem(ModItems.WEB_PORT.get())
		basicItem(ModItems.SCOOCHWORM_GPS.get())
		basicItem(ModItems.SPARKPOLLEN_POUCH.get())
		basicItem(ModItems.TARDIGRADE.get())
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