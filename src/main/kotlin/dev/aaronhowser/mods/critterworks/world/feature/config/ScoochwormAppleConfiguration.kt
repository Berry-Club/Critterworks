package dev.aaronhowser.mods.critterworks.world.feature.config

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.util.valueproviders.IntProvider
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration

data class ScoochwormAppleConfiguration(
	val radius: IntProvider,
	val verticalSearchRange: Int,
	val spawnScoochworm: Boolean
) : FeatureConfiguration {

	companion object {
		val CODEC: Codec<ScoochwormAppleConfiguration> = RecordCodecBuilder.create { instance ->
			instance.group(
				IntProvider.codec(1, 8)
					.fieldOf("radius")
					.forGetter(ScoochwormAppleConfiguration::radius),
				Codec.intRange(1, 128)
					.fieldOf("vertical_search_range")
					.forGetter(ScoochwormAppleConfiguration::verticalSearchRange),
				Codec.BOOL
					.fieldOf("spawn_scoochworm")
					.forGetter(ScoochwormAppleConfiguration::spawnScoochworm)
			).apply(instance, ::ScoochwormAppleConfiguration)
		}
	}
}