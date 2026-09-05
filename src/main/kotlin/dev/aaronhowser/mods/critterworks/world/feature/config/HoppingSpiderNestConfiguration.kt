package dev.aaronhowser.mods.critterworks.world.feature.config

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.util.InclusiveRange
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration

data class HoppingSpiderNestConfiguration(
	val rayAttempts: Int,
	val webCount: InclusiveRange<Int>,
	val webDistance: InclusiveRange<Double>,
	val raySpread: Double,
	val connectionProgress: InclusiveRange<Double>,
	val spiders: InclusiveRange<Int>,
	val minimumDepthBelowSurface: Int,
	val connectWebs: Boolean
) : FeatureConfiguration {
	init {
		require(rayAttempts >= webCount.minInclusive)
		require(webDistance.minInclusive < webDistance.maxInclusive)
	}

	companion object {
		val CODEC: Codec<HoppingSpiderNestConfiguration> = RecordCodecBuilder.create { instance ->
			instance.group(
				Codec.intRange(1, 64)
					.fieldOf("ray_attempts")
					.forGetter(HoppingSpiderNestConfiguration::rayAttempts),
				InclusiveRange.codec(Codec.INT, 1, 6)
					.fieldOf("web_count")
					.forGetter(HoppingSpiderNestConfiguration::webCount),
				InclusiveRange.codec(Codec.DOUBLE, 0.0, 32.0)
					.fieldOf("web_distance")
					.forGetter(HoppingSpiderNestConfiguration::webDistance),
				Codec.doubleRange(0.0, 4.0)
					.fieldOf("ray_spread")
					.forGetter(HoppingSpiderNestConfiguration::raySpread),
				InclusiveRange.codec(Codec.DOUBLE, 0.0, 1.0)
					.fieldOf("connection_progress")
					.forGetter(HoppingSpiderNestConfiguration::connectionProgress),
				InclusiveRange.codec(Codec.INT, 0, 64)
					.fieldOf("spiders")
					.forGetter(HoppingSpiderNestConfiguration::spiders),
				Codec.intRange(1, 64)
					.fieldOf("minimum_depth_below_surface")
					.forGetter(HoppingSpiderNestConfiguration::minimumDepthBelowSurface),
				Codec.BOOL
					.fieldOf("connect_webs")
					.forGetter(HoppingSpiderNestConfiguration::connectWebs)
			).apply(instance, ::HoppingSpiderNestConfiguration)
		}
	}
}