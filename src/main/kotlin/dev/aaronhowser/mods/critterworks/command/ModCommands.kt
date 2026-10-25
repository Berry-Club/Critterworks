package dev.aaronhowser.mods.critterworks.command

import com.mojang.brigadier.CommandDispatcher
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands

object ModCommands {

	fun register(
		dispatcher: CommandDispatcher<CommandSourceStack>,
		buildContext: CommandBuildContext
	) {
		dispatcher.register(
			Commands.literal("critterworks")
				.then(SnailChaseCommands.register())
		)
	}

}