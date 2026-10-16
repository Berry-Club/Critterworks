package dev.aaronhowser.mods.critterworks.client.render.entity.layer

import dev.aaronhowser.mods.critterworks.client.model.entity.ScoochwormLockboxModel
import dev.aaronhowser.mods.critterworks.client.render.entity.ScoochwormPartRenderer
import dev.aaronhowser.mods.critterworks.entity.scoochworm.ScoochwormPartEntity
import dev.aaronhowser.mods.critterworks.entity.scoochworm.attachment.builtin.LockboxAttachment
import dev.aaronhowser.mods.critterworks.registry.ModScoochwormAttachmentTypes

class ScoochwormLockboxLayer private constructor(
	renderer: ScoochwormPartRenderer,
	private val lockboxModel: ScoochwormLockboxModel
) : ScoochwormPartAttachmentLayer(
	renderer,
	ModScoochwormAttachmentTypes.LOCKBOX.get(),
	lockboxModel
) {

	constructor(renderer: ScoochwormPartRenderer) : this(
		renderer,
		ScoochwormLockboxModel()
	)

	override fun prepareModel(
		animatable: ScoochwormPartEntity,
		partialTick: Float
	) {
		val lockbox = animatable.getAttachment() as? LockboxAttachment
		lockboxModel.updateLid(lockbox, partialTick)
	}
}