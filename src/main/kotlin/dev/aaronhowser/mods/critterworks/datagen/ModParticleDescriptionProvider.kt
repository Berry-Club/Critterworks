package dev.aaronhowser.mods.critterworks.datagen

import dev.aaronhowser.mods.critterworks.Critterworks
import dev.aaronhowser.mods.critterworks.registry.ModParticleTypes
import net.minecraft.data.PackOutput
import net.neoforged.neoforge.common.data.ExistingFileHelper
import net.neoforged.neoforge.common.data.ParticleDescriptionProvider

class ModParticleDescriptionProvider(
	output: PackOutput,
	existingFileHelper: ExistingFileHelper
) : ParticleDescriptionProvider(output, existingFileHelper) {

	override fun addDescriptions() {
		spriteSet(ModParticleTypes.FIREFLY.get(), Critterworks.modResource("firefly"))
	}

}