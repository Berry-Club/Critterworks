package dev.aaronhowser.mods.critterworks.command

import com.mojang.brigadier.builder.ArgumentBuilder
import dev.aaronhowser.mods.aaron.command.AaronCommandHelper
import dev.aaronhowser.mods.critterworks.handler.snail.EvilSnailHandler
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer

object SnailCommands : AaronCommandHelper {

	const val PLAYER = "player"
	const val POSITION = "position"

	fun register(): ArgumentBuilder<CommandSourceStack, *> {
		return literal("snail") {
			requires { it.hasPermission(2) }

			thenLiteral("evil-chase") {
				thenLiteral("end") {
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

	//TODO: Better logs
	private fun removeFromPlayer(
		source: CommandSourceStack,
		player: ServerPlayer
	): Int {
		val handler = EvilSnailHandler.get(player.serverLevel())

		if (!handler.hasSnail(player)) {
			source.sendFailure(Component.literal("Player does not have a snail chasing them"))
			return 0
		}

		handler.removeSnail(player)
		source.sendSuccess({ Component.literal("Removed player's snail!") }, true)

		return 1
	}

	private fun startChase() {

	}

}