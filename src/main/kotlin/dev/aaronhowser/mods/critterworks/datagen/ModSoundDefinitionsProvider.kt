package dev.aaronhowser.mods.critterworks.datagen

import dev.aaronhowser.mods.critterworks.Critterworks
import dev.aaronhowser.mods.critterworks.datagen.language.ModSoundLang
import dev.aaronhowser.mods.critterworks.registry.ModSoundEvents
import net.minecraft.data.PackOutput
import net.minecraft.sounds.SoundEvents
import net.neoforged.neoforge.common.data.ExistingFileHelper
import net.neoforged.neoforge.common.data.SoundDefinition
import net.neoforged.neoforge.common.data.SoundDefinitionsProvider

class ModSoundDefinitionsProvider(
	output: PackOutput,
	existingFileHelper: ExistingFileHelper
) : SoundDefinitionsProvider(output, Critterworks.MOD_ID, existingFileHelper) {

	override fun registerSounds() {
		val footstepDefinition = definition()
			.subtitle(ModSoundLang.SCOOCHWORM_FOOTSTEP_SUBTITLE)
			.with(
				sound(Critterworks.modResource("entity/scoochworm/footstep_1"))
			)
			.with(
				sound(Critterworks.modResource("entity/scoochworm/footstep_2"))
			)

		add(ModSoundEvents.SCOOCHWORM_FOOTSTEP, footstepDefinition)

		val kissDefinition = definition()
			.subtitle(ModSoundLang.SCOOCHWORM_KISS_SUBTITLE)
			.with(
				sound(Critterworks.modResource("entity/kiss_1"))
			)
			.with(
				sound(Critterworks.modResource("entity/kiss_2"))
			)
			.with(
				sound(Critterworks.modResource("entity/kiss_3"))
			)

		add(ModSoundEvents.SCOOCHWORM_KISS, kissDefinition)

		val webSnapDefinition = definition()
			.subtitle(ModSoundLang.WEB_SNAP_SUBTITLE)
			.with(
				sound(
					SoundEvents.ARROW_SHOOT.location,
					SoundDefinition.SoundType.EVENT
				)
			)

		add(ModSoundEvents.WEB_SNAP, webSnapDefinition)
	}

}