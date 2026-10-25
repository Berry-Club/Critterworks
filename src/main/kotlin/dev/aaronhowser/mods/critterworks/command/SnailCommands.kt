package dev.aaronhowser.mods.critterworks.command

import com.mojang.brigadier.builder.ArgumentBuilder
import dev.aaronhowser.mods.aaron.command.AaronCommandHelper
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.world.entity.player.Player

object SnailCommands : AaronCommandHelper {

	const val PLAYER = "player"
	const val POSITION = "position"

	fun register(): ArgumentBuilder<CommandSourceStack, *> {
		return literal("snail") {
			requires { it.hasPermission(2) }

			thenLiteral("evil") {
				thenLiteral("remove") {
					executes {
						val player = it.source.playerOrException
						removeFromPlayer(it.source, player)
					}

					thenArgument(PLAYER, EntityArgument.player()) {
						executes {
							val player = EntityArgument.getPlayer(it, PLAYER)
							removeFromPlayer(it.source, player)
						}
					}
				}
			}
		}
	}

	private fun removeFromPlayer(
		source: CommandSourceStack,
		player: Player
	): Int {

		return 1
	}

}