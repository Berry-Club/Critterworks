package dev.aaronhowser.mods.critterworks.datagen.model

import dev.aaronhowser.mods.aaron.misc.AaronDsls.element
import dev.aaronhowser.mods.aaron.misc.AaronDsls.face
import dev.aaronhowser.mods.aaron.misc.AaronDsls.transform
import dev.aaronhowser.mods.aaron.misc.AaronDsls.transforms
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.particle
import dev.aaronhowser.mods.critterworks.Critterworks
import dev.aaronhowser.mods.critterworks.block.CritterCageBlock
import dev.aaronhowser.mods.critterworks.block.DyeberryVinesBlock
import dev.aaronhowser.mods.critterworks.block.ScoochstemBlock
import dev.aaronhowser.mods.critterworks.entity.data.WormColor
import dev.aaronhowser.mods.critterworks.registry.ModBlocks
import dev.aaronhowser.mods.critterworks.registry.ModItems
import net.minecraft.client.renderer.RenderType
import net.minecraft.core.Direction
import net.minecraft.data.PackOutput
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.CaveVines
import net.minecraft.world.level.block.HugeMushroomBlock
import net.minecraft.world.level.block.RotatedPillarBlock
import net.neoforged.neoforge.client.model.generators.*
import net.neoforged.neoforge.common.data.ExistingFileHelper

class ModBlockStateProvider(
	output: PackOutput,
	existingFileHelper: ExistingFileHelper
) : BlockStateProvider(output, Critterworks.MOD_ID, existingFileHelper) {

	override fun registerStatesAndModels() {
		critterCage()
		hoppingSpiderNest()
		appleSlice()
		scoochstem()
		scoochwormDepot()
		stemEncasedComparator()
		coloredScoochstem()
		dyeberryVines()
	}

	private fun hoppingSpiderNest() {
		val block = ModBlocks.HOPPING_SPIDER_NEST.get()
		val model = models().cubeBottomTop(
			"hopping_spider_nest",
			mcLoc("block/bee_nest_side"),
			mcLoc("block/bee_nest_bottom"),
			mcLoc("block/bee_nest_top")
		)
		simpleBlockWithItem(block, model)
	}

	private fun scoochwormDepot() {
		val block = ModBlocks.SCOOCHWORM_DEPOT.get()
		val model = models().cubeAll("scoochworm_depot", mcLoc("block/iron_block"))
		simpleBlockWithItem(block, model)
	}

	private fun dyeberryVines() {
		val headModel = models()
			.cross("dyeberry_vines", mcLoc("block/cave_vines"))
			.renderType(RenderType.CUTOUT.name)
		val plantModel = models()
			.cross("dyeberry_vines_plant", mcLoc("block/cave_vines_plant"))
			.renderType(RenderType.CUTOUT.name)

		val headBuilder = getVariantBuilder(ModBlocks.DYEBERRY_VINES.get())
		headBuilder
			.partialState()
			.with(CaveVines.BERRIES, false)
			.modelForState()
			.modelFile(headModel)
			.addModel()

		val plantBuilder = getVariantBuilder(ModBlocks.DYEBERRY_VINES_PLANT.get())
		plantBuilder
			.partialState()
			.with(CaveVines.BERRIES, false)
			.modelForState()
			.modelFile(plantModel)
			.addModel()

		for (color in WormColor.entries) {
			val colorName = color.colorName
			val headLitModel = models()
				.cross("dyeberry_vines_${colorName}_lit", modLoc("block/dyeberry_vines/${colorName}_lit"))
				.renderType(RenderType.CUTOUT.name)
			val plantLitModel = models()
				.cross(
					"dyeberry_vines_${colorName}_plant_lit",
					modLoc("block/dyeberry_vines/${colorName}_plant_lit")
				)
				.renderType(RenderType.CUTOUT.name)

			headBuilder
				.partialState()
				.with(CaveVines.BERRIES, true)
				.with(DyeberryVinesBlock.COLOR, color)
				.modelForState()
				.modelFile(headLitModel)
				.addModel()

			plantBuilder
				.partialState()
				.with(CaveVines.BERRIES, true)
				.with(DyeberryVinesBlock.COLOR, color)
				.modelForState()
				.modelFile(plantLitModel)
				.addModel()
		}
	}

	private fun critterCage() {
		val closedModel = ModelFile.UncheckedModelFile(modLoc("block/critter_cage"))
		val openModel = ModelFile.UncheckedModelFile(modLoc("block/critter_cage_open"))

		getVariantBuilder(ModBlocks.CRITTER_CAGE.get()).forAllStates { state ->
			val down = state.getValue(CritterCageBlock.DOWN)
			val forward = state.getValue(CritterCageBlock.FORWARD)
			val model = if (state.getValue(CritterCageBlock.OPEN)) {
				openModel
			} else {
				closedModel
			}
			val configuredModel = ConfiguredModel.builder().modelFile(model)

			when (down) {
				Direction.DOWN -> configuredModel
					.rotationY(horizontalRotation(forward))

				Direction.UP -> configuredModel
					.rotationX(180)
					.rotationY((horizontalRotation(forward) + 180) % 360)

				Direction.NORTH -> configuredModel.rotationX(90)
				Direction.EAST -> configuredModel.rotationX(90).rotationY(90)
				Direction.SOUTH -> configuredModel.rotationX(90).rotationY(180)
				Direction.WEST -> configuredModel.rotationX(90).rotationY(270)
			}

			configuredModel.build()
		}

		itemModels()
			.getBuilder(ModItems.CRITTER_CAGE.id.path)
			.parent(ModelFile.UncheckedModelFile("builtin/entity"))
			.transforms {
				transform(ItemDisplayContext.GUI) {
					rotation(30f, 225f, 0f)
					scale(0.625f)
				}

				transform(ItemDisplayContext.GROUND) {
					translation(0f, 3f, 0f)
					scale(0.25f)
				}

				transform(ItemDisplayContext.FIXED) {
					scale(0.5f)
				}

				transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
					rotation(75f, 45f, 0f)
					translation(0f, 2.5f, 0f)
					scale(0.375f)
				}

				transform(ItemDisplayContext.THIRD_PERSON_LEFT_HAND) {
					rotation(75f, 45f, 0f)
					translation(0f, 2.5f, 0f)
					scale(0.375f)
				}

				transform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) {
					rotation(0f, 45f, 0f)
					scale(0.4f)
				}

				transform(ItemDisplayContext.FIRST_PERSON_LEFT_HAND) {
					rotation(0f, 45f, 0f)
					scale(0.4f)
				}
			}
	}

	private fun horizontalRotation(direction: Direction): Int {
		return when (direction) {
			Direction.NORTH -> 0
			Direction.EAST -> 90
			Direction.SOUTH -> 180
			Direction.WEST -> 270
			else -> 0
		}
	}

	private fun appleSlice() {
		val block = ModBlocks.APPLE_SLICE.get()
		val outsideModel = models().getExistingFile(mcLoc("block/red_mushroom_block"))
		val insideModel = models().getExistingFile(mcLoc("block/mushroom_block_inside"))

		for (direction in Direction.entries) {
			val property = when (direction) {
				Direction.NORTH -> HugeMushroomBlock.NORTH
				Direction.EAST -> HugeMushroomBlock.EAST
				Direction.SOUTH -> HugeMushroomBlock.SOUTH
				Direction.WEST -> HugeMushroomBlock.WEST
				Direction.UP -> HugeMushroomBlock.UP
				Direction.DOWN -> HugeMushroomBlock.DOWN
			}

			getMultipartBuilder(block)
				.part()
				.modelFile(outsideModel)
				.rotationX(getFaceXRotation(direction))
				.rotationY(getFaceYRotation(direction))
				.addModel()
				.condition(property, true)
				.end()

			getMultipartBuilder(block)
				.part()
				.modelFile(insideModel)
				.rotationX(getFaceXRotation(direction))
				.rotationY(getFaceYRotation(direction))
				.addModel()
				.condition(property, false)
				.end()
		}

		simpleBlockItem(block, models().cubeAll("apple_slice", mcLoc("block/red_mushroom_block")))
	}

	private fun coloredScoochstem() {
		val topTexture = modLoc("block/scoochstem/top")
		val blocks = listOf(
			ModBlocks.GREEN_SCOOCHSTEM.get(),
			ModBlocks.BLUE_SCOOCHSTEM.get(),
			ModBlocks.RED_SCOOCHSTEM.get(),
			ModBlocks.YELLOW_SCOOCHSTEM.get(),
			ModBlocks.MAGENTA_SCOOCHSTEM.get(),
			ModBlocks.CYAN_SCOOCHSTEM.get()
		)

		for (block in blocks) {
			val color = block.color
			val name = "${color.colorName}_scoochstem"
			val sideTexture = modLoc("block/colored_scoochstem/${color.colorName}")

			val model = models()
				.cube(name, topTexture, topTexture, sideTexture, sideTexture, sideTexture, sideTexture)
				.particle(sideTexture)

			axisBlock(block, model, model)
			simpleBlockItem(block, model)
		}
	}

	private fun scoochstem() {
		val scoochstem = ModBlocks.SCOOCHSTEM.get()
		val side = modLoc("block/scoochstem/side")
		val sideDisabled = modLoc("block/scoochstem/side_disabled")
		val top = modLoc("block/scoochstem/top")
		val topDisabled = modLoc("block/scoochstem/top_disabled")

		val sideModels =
			scoochstemFaceModels("scoochstem_side", side)
		val disabledSideModels =
			scoochstemFaceModels("scoochstem_side_disabled", sideDisabled)
		val endModels =
			scoochstemFaceModels("scoochstem_top", top)
		val disabledEndModels =
			scoochstemFaceModels("scoochstem_top_disabled", topDisabled)

		scoochstemBlock(
			block = scoochstem,
			sideModels = sideModels,
			disabledSideModels = disabledSideModels,
			endModels = endModels,
			disabledEndModels = disabledEndModels
		)

		val itemModel = models()
			.cube("scoochstem", top, top, side, side, side, side)
			.particle(side)

		simpleBlockItem(scoochstem, itemModel)

		val scoochstemWood = ModBlocks.SCOOCHSTEM_WOOD.get()
		scoochstemBlock(
			block = scoochstemWood,
			sideModels = sideModels,
			disabledSideModels = disabledSideModels,
			endModels = sideModels,
			disabledEndModels = disabledSideModels
		)

		val woodItemModel = models()
			.cubeAll("scoochstem_wood", side)
			.particle(side)

		simpleBlockItem(scoochstemWood, woodItemModel)
	}

	private fun stemEncasedComparator() {
		val stemEncasedComparator = ModBlocks.STEM_ENCASED_COMPARATOR.get()

		val side = modLoc("block/stem_encased_comparator/side")
		val sideDisabled = modLoc("block/stem_encased_comparator/side_disabled")
		val top = modLoc("block/stem_encased_comparator/top")
		val topDisabled = modLoc("block/stem_encased_comparator/top_disabled")

		scoochstemBlock(
			block = stemEncasedComparator,
			sideModels = scoochstemFaceModels("stem_encased_comparator_side", side),
			disabledSideModels = scoochstemFaceModels(
				"stem_encased_comparator_side_disabled",
				sideDisabled
			),
			endModels = scoochstemFaceModels("stem_encased_comparator_top", top),
			disabledEndModels = scoochstemFaceModels(
				"stem_encased_comparator_top_disabled",
				topDisabled
			)
		)

		val comparatorItemModel = models()
			.cube(
				"stem_encased_comparator",
				top,
				top,
				side,
				side,
				side,
				side
			)
			.particle(side)

		simpleBlockItem(stemEncasedComparator, comparatorItemModel)
	}

	private fun scoochstemBlock(
		block: Block,
		sideModels: Pair<BlockModelBuilder, BlockModelBuilder>,
		disabledSideModels: Pair<BlockModelBuilder, BlockModelBuilder>,
		endModels: Pair<BlockModelBuilder, BlockModelBuilder>,
		disabledEndModels: Pair<BlockModelBuilder, BlockModelBuilder>
	) {
		fun addScoochstemFace(
			direction: Direction,
			axis: Direction.Axis,
			disabled: Boolean,
			faceModels: Pair<BlockModelBuilder, BlockModelBuilder>
		) {
			val shouldRotateTexture = when {
				direction.axis == axis -> axis != Direction.Axis.Y
				axis == Direction.Axis.Y -> false
				direction.axis == Direction.Axis.Y -> axis == Direction.Axis.X
				else -> true
			}

			val faceModel = if (shouldRotateTexture) {
				faceModels.second
			} else {
				faceModels.first
			}

			getMultipartBuilder(block)
				.part()
				.modelFile(faceModel)
				.rotationX(getFaceXRotation(direction))
				.rotationY(getFaceYRotation(direction))
				.addModel()
				.condition(RotatedPillarBlock.AXIS, axis)
				.condition(ScoochstemBlock.getDisabledProperty(direction), disabled)
				.end()
		}

		for (direction in Direction.entries) {
			for (axis in Direction.Axis.entries) {
				val isPillarEnd = direction.axis == axis
				val enabledModels =
					if (isPillarEnd) endModels else sideModels
				val disabledModels =
					if (isPillarEnd) disabledEndModels else disabledSideModels

				addScoochstemFace(
					direction = direction,
					axis = axis,
					disabled = false,
					faceModels = enabledModels
				)
				addScoochstemFace(
					direction = direction,
					axis = axis,
					disabled = true,
					faceModels = disabledModels
				)
			}
		}
	}

	private fun scoochstemFaceModels(
		name: String,
		texture: ResourceLocation
	): Pair<BlockModelBuilder, BlockModelBuilder> {
		fun scoochstemFaceModel(
			modelName: String,
			rotateTexture: Boolean
		): BlockModelBuilder {
			return models()
				.withExistingParent(modelName, mcLoc("block/block"))
				.texture("texture", texture)
				.particle(texture)
				.element {
					from(0f, 0f, 0f)
					to(16f, 16f, 16f)
					face(Direction.NORTH) {
						texture("#texture")
						cullface(Direction.NORTH)

						if (rotateTexture) {
							rotation(ModelBuilder.FaceRotation.CLOCKWISE_90)
						}
					}
				}
		}

		val regularModel = scoochstemFaceModel(name, false)
		val rotatedModel = scoochstemFaceModel(name + "_rotated", true)

		return regularModel to rotatedModel
	}

	private fun getFaceXRotation(direction: Direction): Int {
		return when (direction) {
			Direction.UP -> 270
			Direction.DOWN -> 90
			else -> 0
		}
	}

	private fun getFaceYRotation(direction: Direction): Int {
		return when (direction) {
			Direction.NORTH -> 0
			Direction.EAST -> 90
			Direction.SOUTH -> 180
			Direction.WEST -> 270
			else -> 0
		}
	}
}