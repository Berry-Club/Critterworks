package dev.aaronhowser.mods.critterworks.command

import com.mojang.brigadier.builder.ArgumentBuilder
import dev.aaronhowser.mods.aaron.command.AaronCommandHelper
import dev.aaronhowser.mods.critterworks.handler.snail.EvilSnailHandler
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.commands.arguments.coordinates.Vec3Argument
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.phys.Vec3

//TODO: Better logs
object SnailChaseCommands : AaronCommandHelper {

	const val PLAYER = "player"
	const val POSITION = "position"

	fun register(): ArgumentBuilder<CommandSourceStack, *> {
		return literal("snail-chase") {
			requires { it.hasPermission(2) }

			thenLiteral("end") {
				executes {
					val player = it.source.playerOrException
					endChase(it.source, player)
				}

				thenArgument(PLAYER, EntityArgument.player()) {
					executes {
						val player = EntityArgument.getPlayer(it, PLAYER)
						endChase(it.source, player)
					}
				}
			}

			thenLiteral("start") {
				executes {
					val player = it.source.playerOrException
					val pos = player.eyePosition.add(10.0, 0.0, 0.0)
					startChase(it.source, player, pos)
				}

				thenArgument(PLAYER, EntityArgument.player()) {
					executes {
						val player = EntityArgument.getPlayer(it, PLAYER)
						val pos = player.eyePosition.add(10.0, 0.0, 0.0)
						startChase(it.source, player, pos)
					}

					thenArgument(POSITION, Vec3Argument.vec3()) {
						executes {
							val player = EntityArgument.getPlayer(it, PLAYER)
							val pos = Vec3Argument.getVec3(it, POSITION)
							startChase(it.source, player, pos)
						}
					}
				}
			}

			thenLiteral("info") {
				executes {
					val player = it.source.playerOrException
					printInfo(it.source, player)
				}

				thenArgument(PLAYER, EntityArgument.player()) {
					executes {
						val player = EntityArgument.getPlayer(it, PLAYER)
						printInfo(it.source, player)
					}
				}
			}
		}
	}

	private fun endChase(
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

	private fun startChase(
		source: CommandSourceStack,
		player: ServerPlayer,
		position: Vec3
	): Int {
		val handler = EvilSnailHandler.get(player.serverLevel())

		if (handler.hasSnail(player)) {
			source.sendFailure(Component.literal("Player already has a snail chasing them"))
			return 0
		}

		handler.addSnail(player, position)
		source.sendSuccess({ Component.literal("Added player's snail!") }, true)

		return 1
	}

	@Suppress("SameReturnValue")
	private fun printInfo(
		source: CommandSourceStack,
		player: ServerPlayer
	): Int {
		val handler = EvilSnailHandler.get(player.serverLevel())
		val snail = handler.getSnail(player)

		if (snail == null) {
			source.sendSuccess({ Component.literal("Player does not have a snail chasing them") }, false)
			return 1
		}

		val bPos = BlockPos.containing(snail.pos)

		val component = if (snail.dimension == player.level().dimension()) {
			val distance = snail.pos.distanceTo(player.position()).toInt()
			Component.literal("Snail is at ${bPos.x}, ${bPos.y}, ${bPos.z}, $distance blocks away")
		} else {
			Component.literal("Snail is at ${bPos.x}, ${bPos.y}, ${bPos.z} in ${snail.dimension.location()}")
		}

		source.sendSuccess({ component }, true)

		return 1
	}

}