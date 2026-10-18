package dev.aaronhowser.mods.critterworks.client.render.entity

import dev.aaronhowser.mods.critterworks.client.model.entity.SnailShellModel
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.player.Player
import net.neoforged.neoforge.client.event.RenderPlayerEvent
import software.bernie.geckolib.animatable.GeoReplacedEntity
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.animatable.instance.SingletonAnimatableInstanceCache
import software.bernie.geckolib.animation.AnimatableManager
import software.bernie.geckolib.renderer.GeoReplacedEntityRenderer

class SnailShellRenderer(
	context: EntityRendererProvider.Context
) : GeoReplacedEntityRenderer<Player, SnailShellRenderer.ShellAnimatable>(
	context,
	SnailShellModel(),
	ShellAnimatable
) {

	object ShellAnimatable : GeoReplacedEntity {
		private val animatableInstanceCache = SingletonAnimatableInstanceCache(this)
		override fun getReplacingEntityType(): EntityType<*> = EntityType.PLAYER
		override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar) {}
		override fun getAnimatableInstanceCache(): AnimatableInstanceCache = animatableInstanceCache
	}

	companion object {
		private var renderer: SnailShellRenderer? = null

		fun initialize(context: EntityRendererProvider.Context) {
			renderer = SnailShellRenderer(context)
		}

		fun render(event: RenderPlayerEvent.Pre): Boolean {
			val shellRenderer = renderer ?: return false
			val player = event.entity

			shellRenderer.render(
				player,
				player.yBodyRot,
				event.partialTick,
				event.poseStack,
				event.multiBufferSource,
				event.packedLight
			)

			return true
		}
	}

}