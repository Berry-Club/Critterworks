package dev.aaronhowser.mods.critterworks.datagen

import dev.aaronhowser.mods.critterworks.Critterworks
import dev.aaronhowser.mods.critterworks.datagen.language.ModLanguageProvider
import dev.aaronhowser.mods.critterworks.datagen.loot.ModLootTableProvider
import dev.aaronhowser.mods.critterworks.datagen.model.ModBlockStateProvider
import dev.aaronhowser.mods.critterworks.datagen.model.ModItemModelProvider
import dev.aaronhowser.mods.critterworks.datagen.sound.ModSoundDefinitionsProvider
import dev.aaronhowser.mods.critterworks.datagen.tag.ModBlockTagsProvider
import dev.aaronhowser.mods.critterworks.datagen.tag.ModBiomeTagsProvider
import dev.aaronhowser.mods.critterworks.datagen.tag.ModEntityTypeTagsProvider
import dev.aaronhowser.mods.critterworks.datagen.tag.ModItemTagsProvider
import dev.aaronhowser.mods.critterworks.datagen.tag.ModMobEffectTagsProvider
import dev.aaronhowser.mods.critterworks.datagen.worldgen.ModBiomeModifiers
import dev.aaronhowser.mods.critterworks.datagen.worldgen.ModConfiguredFeatures
import dev.aaronhowser.mods.critterworks.datagen.worldgen.ModPlacedFeatures
import net.minecraft.core.RegistrySetBuilder
import net.minecraft.core.registries.Registries
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.common.data.AdvancementProvider
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider
import net.neoforged.neoforge.data.event.GatherDataEvent
import net.neoforged.neoforge.registries.NeoForgeRegistries

@EventBusSubscriber(modid = Critterworks.MOD_ID)
object ModDataGen {

	@SubscribeEvent
	fun onGatherData(event: GatherDataEvent) {
		val generator = event.generator
		val output = generator.packOutput

		val lookupProvider = event.lookupProvider
		val existingFileHelper = event.existingFileHelper

		generator.addProvider(
			event.includeServer(),
			DatapackBuiltinEntriesProvider(
				output,
				lookupProvider,
				RegistrySetBuilder()
					.add(Registries.CONFIGURED_FEATURE, ModConfiguredFeatures::bootstrap)
					.add(Registries.PLACED_FEATURE, ModPlacedFeatures::bootstrap)
					.add(NeoForgeRegistries.Keys.BIOME_MODIFIERS, ModBiomeModifiers::bootstrap),
				setOf(Critterworks.MOD_ID)
			)
		)

		generator.addProvider(
			event.includeServer(),
			ModRecipeProvider(output, lookupProvider)
		)

		generator.addProvider(
			event.includeClient(),
			ModBlockStateProvider(output, existingFileHelper)
		)

		generator.addProvider(
			event.includeClient(),
			ModItemModelProvider(output, existingFileHelper)
		)

		generator.addProvider(
			event.includeClient(),
			ModLanguageProvider(output)
		)

		generator.addProvider(
			event.includeClient(),
			ModSoundDefinitionsProvider(output, existingFileHelper)
		)

		generator.addProvider(
			event.includeServer(),
			ModLootTableProvider(output, lookupProvider)
		)

		generator.addProvider(
			event.includeServer(),
			AdvancementProvider(
				output,
				lookupProvider,
				existingFileHelper,
				listOf(ModAdvancementSubProvider(lookupProvider))
			)
		)

		val blockTagProvider = generator.addProvider(
			event.includeServer(),
			ModBlockTagsProvider(output, lookupProvider, existingFileHelper)
		)

		generator.addProvider(
			event.includeServer(),
			ModItemTagsProvider(output, lookupProvider, blockTagProvider.contentsGetter(), existingFileHelper)
		)

		generator.addProvider(
			event.includeServer(),
			ModBiomeTagsProvider(output, lookupProvider, existingFileHelper)
		)

		generator.addProvider(
			event.includeServer(),
			ModEntityTypeTagsProvider(output, lookupProvider)
		)

		generator.addProvider(
			event.includeServer(),
			ModMobEffectTagsProvider(output, lookupProvider, existingFileHelper)
		)
	}
}