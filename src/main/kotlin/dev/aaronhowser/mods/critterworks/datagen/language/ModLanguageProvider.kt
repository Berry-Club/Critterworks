package dev.aaronhowser.mods.critterworks.datagen.language

import dev.aaronhowser.mods.critterworks.Critterworks
import net.minecraft.data.PackOutput
import net.neoforged.neoforge.common.data.LanguageProvider

class ModLanguageProvider(
	output: PackOutput
) : LanguageProvider(output, Critterworks.MOD_ID, "en_us") {

	override fun addTranslations() {
		ModAdvancementLang.add(this)
		ModBlockLang.add(this)
		ModConfigLang.add(this)
		ModEffectLang.add(this)
		ModEntityLang.add(this)
		ModItemLang.add(this)
		ModMenuLang.add(this)
		ModPotionLang.add(this)
		ModSoundLang.add(this)
		ModMessageLang.add(this)
	}
}