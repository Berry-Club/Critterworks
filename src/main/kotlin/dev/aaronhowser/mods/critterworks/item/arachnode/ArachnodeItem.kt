package dev.aaronhowser.mods.critterworks.item.arachnode

import dev.aaronhowser.mods.critterworks.handler.web.redstone.node.Arachnode
import net.minecraft.world.item.Item

abstract class ArachnodeItem(properties: Properties) : Item(properties) {

	abstract fun createArachnode(context: ArachnodePlacementContext): Arachnode?
}