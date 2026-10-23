package dev.aaronhowser.mods.critterworks.client.item.critter_cage

import com.mojang.blaze3d.vertex.PoseStack
import dev.aaronhowser.mods.aaron.client.AaronClientUtil
import dev.aaronhowser.mods.critterworks.client.critter_cage.CritterCageEntityRenderer
import dev.aaronhowser.mods.critterworks.item.CritterCageItem
import dev.aaronhowser.mods.critterworks.registry.ModBlocks
import dev.aaronhowser.mods.critterworks.registry.ModDataComponents
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.core.Direction
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.level.Level
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions

class CritterCageItemRenderer : BlockEntityWithoutLevelRenderer(
	Minecraft.getInstance().blockEntityRenderDispatcher,
	Minecraft.getInstance().entityModels
) {

	private var cachedData: CustomData? = null
	private var cachedLevel: Level? = null
	private var cachedEntity: LivingEntity? = null

	override fun renderByItem(
		stack: ItemStack,
		displayContext: ItemDisplayContext,
		poseStack: PoseStack,
		buffer: MultiBufferSource,
		packedLight: Int,
		packedOverlay: Int
	) {
		Minecraft.getInstance().blockRenderer.renderSingleBlock(
			ModBlocks.CRITTER_CAGE.get().defaultBlockState(),
			poseStack,
			buffer,
			packedLight,
			packedOverlay
		)

		val entity = getEntity(stack) ?: return

		CritterCageEntityRenderer.render(
			entity,
			Direction.DOWN,
			Direction.NORTH,
			0f,
			poseStack,
			buffer,
			packedLight
		)
	}

	private fun getEntity(stack: ItemStack): LivingEntity? {
		val entityData = stack.get(ModDataComponents.ENTITY_DATA) ?: return null
		val level = AaronClientUtil.localLevel ?: return null

		if (entityData == cachedData && level === cachedLevel) {
			return cachedEntity
		}

		val entity = CritterCageItem.createEntity(entityData, level)

		cachedData = entityData
		cachedLevel = level
		cachedEntity = entity

		return entity
	}

	object ClientItemExtensions : IClientItemExtensions {
		private val renderer = CritterCageItemRenderer()

		override fun getCustomRenderer(): BlockEntityWithoutLevelRenderer {
			return renderer
		}
	}

}