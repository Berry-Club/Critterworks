package dev.aaronhowser.mods.critterworks.client.render.item

import com.mojang.blaze3d.vertex.PoseStack
import dev.aaronhowser.mods.aaron.misc.AaronDsls.withPose
import dev.aaronhowser.mods.critterworks.entity.ScoochwormEntity
import dev.aaronhowser.mods.critterworks.registry.ModBlocks
import dev.aaronhowser.mods.critterworks.registry.ModDataComponents
import dev.aaronhowser.mods.critterworks.registry.ModEntityTypes
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer
import net.minecraft.client.renderer.MultiBufferSource
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
	private var cachedScoochworm: ScoochwormEntity? = null

	override fun renderByItem(
		stack: ItemStack,
		displayContext: ItemDisplayContext,
		poseStack: PoseStack,
		buffer: MultiBufferSource,
		packedLight: Int,
		packedOverlay: Int
	) {
		renderCage(poseStack, buffer, packedLight, packedOverlay)

		val scoochworm = getScoochworm(stack) ?: return
		scoochworm.yRot = 180f
		scoochworm.yRotO = 180f
		scoochworm.yBodyRot = 180f
		scoochworm.yBodyRotO = 180f

		poseStack.withPose {
			poseStack.translate(0.5, 0.05, 0.5)

			val entityRenderer = Minecraft.getInstance()
				.entityRenderDispatcher
				.getRenderer(scoochworm)

			entityRenderer.render(
				scoochworm,
				scoochworm.yRot,
				0f,
				poseStack,
				buffer,
				packedLight
			)
		}
	}

	private fun renderCage(
		poseStack: PoseStack,
		buffer: MultiBufferSource,
		packedLight: Int,
		packedOverlay: Int
	) {
		poseStack.pushPose()
		Minecraft.getInstance().blockRenderer.renderSingleBlock(
			ModBlocks.CRITTER_CAGE.get().defaultBlockState(),
			poseStack,
			buffer,
			packedLight,
			packedOverlay
		)
		poseStack.popPose()
	}

	private fun getScoochworm(stack: ItemStack): ScoochwormEntity? {
		val entityData = stack.get(ModDataComponents.ENTITY_DATA) ?: return null
		val level = Minecraft.getInstance().level ?: return null

		if (entityData == cachedData && level === cachedLevel) {
			return cachedScoochworm
		}

		val scoochworm = ScoochwormEntity(ModEntityTypes.SCOOCHWORM.get(), level)
		scoochworm.load(entityData.copyTag())

		cachedData = entityData
		cachedLevel = level
		cachedScoochworm = scoochworm

		return scoochworm
	}

	object ClientItemExtensions : IClientItemExtensions {
		private val renderer = CritterCageItemRenderer()

		override fun getCustomRenderer(): BlockEntityWithoutLevelRenderer {
			return renderer
		}
	}

}