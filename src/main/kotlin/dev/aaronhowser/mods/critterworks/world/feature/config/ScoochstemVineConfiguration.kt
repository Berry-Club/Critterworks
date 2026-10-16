package dev.aaronhowser.mods.critterworks.world.feature.config

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.util.valueproviders.IntProvider
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration

data class ScoochstemVineConfiguration(
	val length: IntProvider,
	val surfaceSearchRange: Int,
	val turnChance: Float
) : FeatureConfiguration {

	companion object {
		val CODEC: Codec<ScoochstemVineConfiguration> = RecordCodecBuilder.create { instance ->
			instance.group(
				IntProvider.codec(3, 50)
					.fieldOf("length")
					.forGetter(ScoochstemVineConfiguration::length),
				Codec.intRange(1, 128)
					.fieldOf("surface_search_range")
					.forGetter(ScoochstemVineConfiguration::surfaceSearchRange),
				Codec.floatRange(0f, 1f)
					.fieldOf("turn_chance")
					.forGetter(ScoochstemVineConfiguration::turnChance)
			).apply(instance, ::ScoochstemVineConfiguration)
		}
	}
}