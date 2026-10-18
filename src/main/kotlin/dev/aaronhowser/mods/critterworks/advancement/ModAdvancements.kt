package dev.aaronhowser.mods.critterworks.advancement

import dev.aaronhowser.mods.aaron.registry.actual.AaronCriterionTriggers
import dev.aaronhowser.mods.critterworks.Critterworks
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player

object ModAdvancements {

	val ROOT = guide("root")

	val BREAK_APPLE_SLICE = guide("break_apple_slice")
	val INTERACT_WITH_SCOOCHWORM = guide("interact_with_scoochworm")
	val ATTACH_TO_SCOOCHWORM = guide("attach_to_scoochworm")
	val SPLIT_SCOOCHWORM = guide("split_scoochworm")
	val WITNESS_HEAD_ON_COLLISION = guide("witness_head_on_collision")
	val DYE_SCOOCHWORM = guide("dye_scoochworm")
	val EAT_DYEBERRY = guide("eat_dyeberry")
	val EAT_AARONBERRY = guide("eat_aaronberry")

	val FIND_SPIDER = guide("find_spider")
	val WEB_MAKING = guide("web_making")
	val WEB_PORT = guide("web_port")

	val SPARKBUG_BUSH = guide("sparkbug_bush")
	val USE_SPARKPOLLEN = guide("use_sparkpollen")

	val CATCH_FILTERFEEDER = guide("catch_filterfeeder")
	val APPLY_TARDIGRADE = guide("apply_tardigade")

	fun trigger(player: Player, action: ResourceLocation) {
		if (player !is ServerPlayer) return

		AaronCriterionTriggers.PLAYER_ACTION.get().trigger(player, action)
	}

	private fun guide(path: String): ResourceLocation {
		return Critterworks.modResource("guide/$path")
	}
}