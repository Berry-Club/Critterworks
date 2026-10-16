package dev.aaronhowser.mods.critterworks.client.model.entity

import dev.aaronhowser.mods.critterworks.Critterworks
import dev.aaronhowser.mods.critterworks.entity.scoochworm.ScoochwormPartEntity
import dev.aaronhowser.mods.critterworks.entity.scoochworm.attachment.builtin.LockboxAttachment
import net.minecraft.util.Mth
import software.bernie.geckolib.model.DefaultedEntityGeoModel

class ScoochwormLockboxModel : DefaultedEntityGeoModel<ScoochwormPartEntity>(
	Critterworks.modResource("scoochworm/lockbox")
) {

	fun updateLid(
		lockbox: LockboxAttachment?,
		partialTick: Float
	) {
		val progress = Mth.lerp(
			partialTick,
			lockbox?.previousOpenProgress ?: 0f,
			lockbox?.openProgress ?: 0f
		)

		val easedProgress = 1f - (1f - progress) * (1f - progress) * (1f - progress)

		val top = animationProcessor.getBone("top")
		top.rotX = Mth.HALF_PI * easedProgress
	}
}