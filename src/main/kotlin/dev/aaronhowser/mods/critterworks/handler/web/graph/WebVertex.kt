package dev.aaronhowser.mods.critterworks.handler.web.graph

import net.minecraft.server.level.ServerLevel
import net.minecraft.world.phys.Vec3
import java.util.*

interface WebVertex {

	val uuid: UUID
	val position: Vec3

	fun isLoaded(level: ServerLevel): Boolean

}