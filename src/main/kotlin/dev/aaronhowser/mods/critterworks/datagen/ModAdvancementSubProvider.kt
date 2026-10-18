package dev.aaronhowser.mods.critterworks.datagen

import dev.aaronhowser.mods.aaron.advancement.BlockBrokenTrigger
import dev.aaronhowser.mods.aaron.datagen.AaronAdvancementSubProvider
import dev.aaronhowser.mods.aaron.misc.AaronExtensions.toComponent
import dev.aaronhowser.mods.critterworks.Critterworks
import dev.aaronhowser.mods.critterworks.advancement.ModAdvancements
import dev.aaronhowser.mods.critterworks.datagen.language.ModAdvancementLang
import dev.aaronhowser.mods.critterworks.registry.ModBlocks
import dev.aaronhowser.mods.critterworks.registry.ModDataComponents
import dev.aaronhowser.mods.critterworks.registry.ModEntityTypes
import dev.aaronhowser.mods.critterworks.registry.ModItems
import net.minecraft.advancements.Advancement
import net.minecraft.advancements.AdvancementHolder
import net.minecraft.advancements.AdvancementType
import net.minecraft.advancements.Criterion
import net.minecraft.advancements.critereon.*
import net.minecraft.core.HolderLookup
import net.minecraft.core.component.DataComponentPredicate
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.Unit
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.Items
import net.minecraft.world.level.ItemLike
import net.neoforged.neoforge.common.data.ExistingFileHelper
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer

class ModAdvancementSubProvider(
	lookupProvider: CompletableFuture<HolderLookup.Provider>
) : AaronAdvancementSubProvider(Critterworks.MOD_ID, lookupProvider) {

	override fun generate(
		registries: HolderLookup.Provider,
		saver: Consumer<AdvancementHolder>,
		existingFileHelper: ExistingFileHelper
	) {
		fun Advancement.Builder.save(id: ResourceLocation) = save(saver, id, existingFileHelper)

		val root = advancement()
			.display(
				ModItems.SCOOCHWORM_SPAWN_EGG,
				ModAdvancementLang.ROOT_TITLE.toComponent(),
				ModAdvancementLang.ROOT_DESC.toComponent(),
				Critterworks.modResource("textures/block/scoochstem/side.png"),
				AdvancementType.TASK,
				false,
				false,
				false
			)
			.addCriterion("tick", PlayerTrigger.TriggerInstance.tick())
			.save(ModAdvancements.ROOT)

		val breakAppleSlice = advancement()
			.parent(root)
			.display(
				ModBlocks.APPLE_SLICE,
				ModAdvancementLang.BREAK_APPLE_SLICE_TITLE.toComponent(),
				ModAdvancementLang.BREAK_APPLE_SLICE_DESC.toComponent()
			)
			.addCriterion(
				"break_apple_slice",
				BlockBrokenTrigger.TriggerInstance.block(ModBlocks.APPLE_SLICE.get())
			)
			.save(ModAdvancements.BREAK_APPLE_SLICE)

		val interactWithScoochworm = advancement()
			.parent(breakAppleSlice)
			.display(
				ModItems.SCOOCHWORM_SPAWN_EGG,
				ModAdvancementLang.INTERACT_WITH_SCOOCHWORM_TITLE.toComponent(),
				ModAdvancementLang.INTERACT_WITH_SCOOCHWORM_DESC.toComponent()
			)
			.addCriterion("scoochworm", interactedWith(ModEntityTypes.SCOOCHWORM.get()))
			.addCriterion("scoochworm_segment", interactedWith(ModEntityTypes.SCOOCHWORM_PART.get()))
			.anyRequirements()
			.save(ModAdvancements.INTERACT_WITH_SCOOCHWORM)

		advancement()
			.parent(interactWithScoochworm)
			.display(
				Items.SADDLE,
				ModAdvancementLang.ATTACH_TO_SCOOCHWORM_TITLE.toComponent(),
				ModAdvancementLang.ATTACH_TO_SCOOCHWORM_DESC.toComponent(),
				type = AdvancementType.GOAL
			)
			.addCriterion(
				"attach_to_scoochworm",
				playerAction(ModAdvancements.ATTACH_TO_SCOOCHWORM)
			)
			.save(ModAdvancements.ATTACH_TO_SCOOCHWORM)

		advancement()
			.parent(interactWithScoochworm)
			.display(
				Items.SHEARS,
				ModAdvancementLang.SPLIT_SCOOCHWORM_TITLE.toComponent(),
				ModAdvancementLang.SPLIT_SCOOCHWORM_DESC.toComponent(),
				type = AdvancementType.GOAL
			)
			.addCriterion(
				"split_scoochworm",
				playerAction(ModAdvancements.SPLIT_SCOOCHWORM)
			)
			.save(ModAdvancements.SPLIT_SCOOCHWORM)

		advancement()
			.parent(interactWithScoochworm)
			.display(
				Items.POPPY,
				ModAdvancementLang.WITNESS_HEAD_ON_COLLISION_TITLE.toComponent(),
				ModAdvancementLang.WITNESS_HEAD_ON_COLLISION_DESC.toComponent(),
				type = AdvancementType.CHALLENGE
			)
			.addCriterion(
				"witness_head_on_collision",
				playerAction(ModAdvancements.WITNESS_HEAD_ON_COLLISION)
			)
			.save(ModAdvancements.WITNESS_HEAD_ON_COLLISION)

		advancement()
			.parent(interactWithScoochworm)
			.display(
				ModItems.GREEN_DYEBERRY,
				ModAdvancementLang.DYE_SCOOCHWORM_TITLE.toComponent(),
				ModAdvancementLang.DYE_SCOOCHWORM_DESC.toComponent()
			)
			.addCriterion(
				"dye_scoochworm",
				playerAction(ModAdvancements.DYE_SCOOCHWORM)
			)
			.save(ModAdvancements.DYE_SCOOCHWORM)

		val eatDyeberry = advancement()
			.parent(root)
			.display(
				ModItems.GREEN_DYEBERRY,
				ModAdvancementLang.EAT_DYEBERRY_TITLE.toComponent(),
				ModAdvancementLang.EAT_DYEBERRY_DESC.toComponent()
			)
			.addCriterion("green", consumed(ModItems.GREEN_DYEBERRY.get()))
			.addCriterion("blue", consumed(ModItems.BLUE_DYEBERRY.get()))
			.addCriterion("red", consumed(ModItems.RED_DYEBERRY.get()))
			.addCriterion("yellow", consumed(ModItems.YELLOW_DYEBERRY.get()))
			.addCriterion("magenta", consumed(ModItems.MAGENTA_DYEBERRY.get()))
			.addCriterion("cyan", consumed(ModItems.CYAN_DYEBERRY.get()))
			.anyRequirements()
			.save(ModAdvancements.EAT_DYEBERRY)

		advancement()
			.parent(eatDyeberry)
			.display(
				ModItems.AARONBERRY,
				ModAdvancementLang.EAT_AARONBERRY_TITLE.toComponent(),
				ModAdvancementLang.EAT_AARONBERRY_DESC.toComponent(),
				type = AdvancementType.CHALLENGE
			)
			.addCriterion("eat_aaronberry", consumed(ModItems.AARONBERRY.get()))
			.save(ModAdvancements.EAT_AARONBERRY)

		advancement()
			.parent(root)
			.display(
				ModItems.ITEM_FILTERFEEDER,
				ModAdvancementLang.CATCH_FILTERFEEDER_TITLE.toComponent(),
				ModAdvancementLang.CATCH_FILTERFEEDER_DESC.toComponent(),
			)
			.hasAny(ModItems.ITEM_FILTERFEEDER, ModItems.FLUID_FILTERFEEDER)
			.save(ModAdvancements.CATCH_FILTERFEEDER)

		advancement()
			.parent(root)
			.display(
				ModItems.APPLY_TARDIGRADE,
				ModAdvancementLang.APPLY_TARDIGRADE_TITLE.toComponent(),
				ModAdvancementLang.APPLY_TARDIGRADE_DESC.toComponent(),
			)
			.addCriterion(
				"apply_tardigrade",
				hasItems(
					ItemPredicate.Builder
						.item()
						.hasComponents(
							DataComponentPredicate.builder()
								.expect(ModDataComponents.HAS_TARDIGRADE.get(), Unit.INSTANCE)
								.build()
						)
				)
			)
			.save(ModAdvancements.APPLY_TARDIGRADE)

		val spiderNest = advancement()
			.parent(root)
			.display(
				ModItems.HOPPING_SPIDER,
				ModAdvancementLang.FIND_SPIDER_TITLE.toComponent(),
				ModAdvancementLang.FIND_SPIDER_DESC.toComponent(),
			)
			.hasAny(ModItems.HOPPING_SPIDER, ModBlocks.HOPPING_SPIDER_NEST)
			.save(ModAdvancements.FIND_SPIDER)

		val webMaking = advancement()
			.parent(spiderNest)
			.display(
				ModItems.ARTIFICIAL_SPINNERETS,
				ModAdvancementLang.WEB_MAKING_TITLE.toComponent(),
				ModAdvancementLang.WEB_MAKING_DESC.toComponent(),
			)
			.has(ModItems.ARTIFICIAL_SPINNERETS)
			.save(ModAdvancements.WEB_MAKING)

		advancement()
			.parent(webMaking)
			.display(
				ModItems.ARTIFICIAL_SPINNERETS,
				ModAdvancementLang.WEB_PORT_TITLE.toComponent(),
				ModAdvancementLang.WEB_PORT_DESC.toComponent(),
			)
			.has(ModItems.WEB_PORT)
			.save(ModAdvancements.WEB_PORT)

		val sparkbugBush = advancement()
			.display(
				ModBlocks.SPARKBUG_BUSH,
				ModAdvancementLang.SPARKBUG_BUSH_TITLE.toComponent(),
				ModAdvancementLang.SPARKBUG_BUSH_DESC.toComponent(),
			)
			.has(ModBlocks.SPARKBUG_BUSH)
			.save(ModAdvancements.SPARKBUG_BUSH)

		advancement()
			.display(
				ModItems.SPARKPOLLEN_POUCH,
				ModAdvancementLang.USE_SPARKPOLLEN_TITLE.toComponent(),
				ModAdvancementLang.USE_SPARKPOLLEN_DESC.toComponent(),
			)
			.parent(sparkbugBush)
			.addCriterion(
				"use_sparkpollen",
				ItemUsedOnLocationTrigger.TriggerInstance.itemUsedOnBlock(
					LocationPredicate.Builder.location(),
					ItemPredicate.Builder.item().of(ModItems.SPARKPOLLEN_POUCH)
				)
			)
			.save(ModAdvancements.USE_SPARKPOLLEN)
	}

	private fun interactedWith(entityType: EntityType<*>): Criterion<PlayerInteractTrigger.TriggerInstance> {
		return PlayerInteractTrigger.TriggerInstance.itemUsedOnEntity(
			ItemPredicate.Builder.item(),
			Optional.of(
				EntityPredicate.wrap(
					EntityPredicate.Builder.entity()
						.entityType(EntityTypePredicate.of(entityType))
				)
			)
		)
	}

	private fun consumed(item: ItemLike): Criterion<ConsumeItemTrigger.TriggerInstance> {
		return ConsumeItemTrigger.TriggerInstance.usedItem(
			ItemPredicate.Builder.item().of(item)
		)
	}

}