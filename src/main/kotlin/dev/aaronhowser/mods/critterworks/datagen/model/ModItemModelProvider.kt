package dev.aaronhowser.mods.critterworks.datagen.model

import dev.aaronhowser.mods.aaron.misc.AaronDsls.transform
import dev.aaronhowser.mods.aaron.misc.AaronDsls.transforms
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
		dyeberryItems()
		filterfeederItems()
		hoppingSpider()
		lockbox()
		snailShield()
		webstoneWeaver()
		directionalArachnode()

		spawnEggItem(ModItems.SCOOCHWORM_SPAWN_EGG.get())
		spawnEggItem(ModItems.ITEM_FILTERFEEDER_SPAWN_EGG.get())
		spawnEggItem(ModItems.FLUID_FILTERFEEDER_SPAWN_EGG.get())
		spawnEggItem(ModItems.PUNCHPRAWN_SPAWN_EGG.get())
		spawnEggItem(ModItems.SPROUTSNAIL_SPAWN_EGG.get())
	}

	private fun snailShield() {
		val snailShieldModel = getBuilder("snail_shield")
			.parent(ModelFile.UncheckedModelFile(mcLoc("builtin/entity")))
			.texture("particle", modLoc("entity/snail_shield"))

		snailShieldModel.transforms {
			transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
				rotation(75f, 45f, 0f)
					.translation(0f, 2.5f, 0f)
					.scale(0.375f)
			}

			transform(ItemDisplayContext.THIRD_PERSON_LEFT_HAND) {
				rotation(75f, 225f, 0f)
					.translation(0f, 2.5f, 0f)
					.scale(0.375f)
			}

			transform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) {
				rotation(0f, 45f, 0f)
					.scale(0.4f)
			}

			transform(ItemDisplayContext.FIRST_PERSON_LEFT_HAND) {
				rotation(0f, 225f, 0f)
					.scale(0.4f)
			}

			transform(ItemDisplayContext.GROUND) {
				translation(0f, 3f, 0f)
					.scale(0.25f)
			}

			transform(ItemDisplayContext.GUI) {
				rotation(30f, 225f, 0f)
					.translation(1.5f, -4f, 0f)
					.scale(0.4f)
			}

			transform(ItemDisplayContext.FIXED) {
				scale(0.5f)
			}
		}
	}

	private fun lockbox() {
		val lockboxModel = getBuilder("lockbox")
			.parent(ModelFile.UncheckedModelFile(mcLoc("builtin/entity")))
			.texture("particle", modLoc("item/lockbox"))

		lockboxModel.transforms {
			transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
				rotation(75f, 45f, 0f)
				translation(0f, 2.5f, 0f)
				scale(0.8f)
			}

			transform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) {
				rotation(0f, 135f, 0f)
				scale(0.9f)
			}

			transform(ItemDisplayContext.FIRST_PERSON_LEFT_HAND) {
				rotation(0f, 135f, 0f)
				scale(0.9f)
			}

			transform(ItemDisplayContext.GROUND) {
				translation(0f, 3f, 0f)
				scale(0.55f)
			}

			transform(ItemDisplayContext.GUI) {
				rotation(30f, 225f, 0f)
				translation(0f, -4f, 0f)
				scale(1.4f)
			}
		}
	}

	private fun hoppingSpider() {
		val hoppingSpiderModel = getBuilder("hopping_spider")
			.parent(ModelFile.UncheckedModelFile(mcLoc("builtin/entity")))
			.texture("particle", modLoc("item/hopping_spider"))

		hoppingSpiderModel.transforms {
			transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
				rotation(64f, 0f, 0f)
				translation(-0.1f, 1f, -0.75f)
				scale(0.4f)
			}

			transform(ItemDisplayContext.THIRD_PERSON_LEFT_HAND) {
				rotation(64f, 0f, 0f)
				translation(0.1f, 0f, -0.75f)
				scale(0.4f)
			}

			transform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) {
				rotation(-180f, 65f, -180f)
				scale(0.5f)
			}

			transform(ItemDisplayContext.FIRST_PERSON_LEFT_HAND) {
				rotation(-180f, 65f, 180f)
				scale(0.5f)
			}

			transform(ItemDisplayContext.GROUND) {
				scale(0.5f)
			}

			transform(ItemDisplayContext.GUI) {
				rotation(-152.25f, 29.25f, -180f)
				translation(0f, -4f, 0f)
			}

			transform(ItemDisplayContext.HEAD) {
				rotation(90f, 0f, -180f)
				translation(0f, 0f, -4.5f)
			}

			transform(ItemDisplayContext.FIXED) {
				rotation(90f, 0f, -180f)
			}
		}
	}

	private fun basicItems() {
		basicItem(ModItems.ARTIFICIAL_SPINNERETS.get())
		basicItem(ModItems.WEB_PORT.get())
		basicItem(ModItems.SCOOCHWORM_GPS.get())
		basicItem(ModItems.SPARKPOLLEN_POUCH.get())
		basicItem(ModItems.TARDIGRADE.get())
		basicItem(ModItems.PEARL.get())
		basicItem(ModItems.SNAIL_SLIME.get())
		basicItem(ModItems.SNAIL_SHELL_FRAGMENT.get())
	}

	private fun webstoneWeaver() {
		withExistingParent("webstone_weaver", mcLoc("item/generated"))
			.texture("layer0", modLoc("item/artificial_spinnerets"))
	}

	private fun directionalArachnode() {
		withExistingParent("directional_arachnode", mcLoc("item/generated"))
			.texture("layer0", modLoc("item/hopping_spider"))
			.texture("layer1", mcLoc("item/spectral_arrow"))
	}

	private fun filterfeederItems() {
		filterfeederItem(ModItems.ITEM_FILTERFEEDER.get())
		filterfeederItem(ModItems.FLUID_FILTERFEEDER.get())
	}

	private fun filterfeederItem(item: Item) {
		val itemName = BuiltInRegistries.ITEM.getKey(item).path
		val model = getBuilder(itemName)
			.parent(ModelFile.UncheckedModelFile(mcLoc("builtin/entity")))

		model.transforms {
			transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
				rotation(75f, 45f, 0f)
					.translation(0f, 2.5f, 0f)
					.scale(0.375f)
			}

			transform(ItemDisplayContext.THIRD_PERSON_LEFT_HAND) {
				rotation(75f, 45f, 0f)
					.translation(0f, 2.5f, 0f)
					.scale(0.375f)
			}

			transform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) {
				rotation(0f, 45f, 0f)
					.scale(0.4f)
			}

			transform(ItemDisplayContext.FIRST_PERSON_LEFT_HAND) {
				rotation(0f, 225f, 0f)
					.scale(0.4f)
			}

			transform(ItemDisplayContext.GROUND) {
				translation(0f, 3f, 0f)
					.scale(0.25f)
			}

			transform(ItemDisplayContext.GUI) {
				rotation(30f, 225f, 0f)
					.scale(0.625f)
			}

			transform(ItemDisplayContext.FIXED) {
				scale(0.5f)
			}
		}
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