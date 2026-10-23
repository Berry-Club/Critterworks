package dev.aaronhowser.mods.critterworks.client.entity.snail_shell

import dev.aaronhowser.mods.aaron.misc.AaronDsls.withPose
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.player.Player
import net.neoforged.neoforge.client.event.RenderPlayerEvent
import software.bernie.geckolib.animatable.GeoReplacedEntity
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.animation.AnimatableManager
import software.bernie.geckolib.renderer.GeoReplacedEntityRenderer
import software.bernie.geckolib.util.GeckoLibUtil

class SnailShellRenderer(
	context: EntityRendererProvider.Context
) : GeoReplacedEntityRenderer<Player, SnailShellRenderer.ShellAnimatable>(
	context,
	SnailShellModel(),
	ShellAnimatable
) {
	init {
		addRenderLayer(SnailShellHeadLayer(this, context))
	}

	object ShellAnimatable : GeoReplacedEntity {
		private val animatableInstanceCache = GeckoLibUtil.createInstanceCache(this)
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
			val poseStack = event.poseStack

			poseStack.withPose {
				poseStack.translate(0f, 2f / 16f, 0f)
				shellRenderer.render(
					player,
					player.yBodyRot,
					event.partialTick,
					poseStack,
					event.multiBufferSource,
					event.packedLight
				)
			}

			return true
		}
	}

}