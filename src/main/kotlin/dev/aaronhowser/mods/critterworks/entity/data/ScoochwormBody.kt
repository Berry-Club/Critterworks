package dev.aaronhowser.mods.critterworks.entity.data

import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isClientSide
import dev.aaronhowser.mods.critterworks.entity.ScoochwormEntity
import dev.aaronhowser.mods.critterworks.entity.ScoochwormPartEntity
import dev.aaronhowser.mods.critterworks.entity.attachment.ScoochwormAttachmentType
import net.minecraft.nbt.ListTag
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack

class ScoochwormBody(
	private val scoochworm: ScoochwormEntity
) {

	val segments: List<ScoochwormSegment>
		field = mutableListOf()

	init {
		val firstSegment = ScoochwormSegment()
		firstSegment.assignBody(this)
		segments.add(firstSegment)
	}

	val size: Int
		get() = segments.size

	val canGrow: Boolean
		get() = segments.size < MAX_COUNT

	fun contains(partIndex: Int): Boolean {
		return partIndex in segments.indices
	}

	fun grow() {
		if (canGrow) {
			val segment = ScoochwormSegment()
			segment.assignBody(this)
			segments.add(segment)
		}
	}

	fun canSplitAt(partIndex: Int): Boolean {
		return partIndex in MIN_COUNT until segments.lastIndex
	}

	fun splitAt(partIndex: Int): ScoochwormEntity? {
		if (!canSplitAt(partIndex)) return null

		val previousTail = segments.last()
		val previousTailBodyPart = previousTail.bodyPart ?: return null
		val newHeadPosition = previousTailBodyPart.position()
		val newSupportDirection = previousTailBodyPart.supportDirection
		val newYaw = previousTailBodyPart.yRot + 180f

		val transferredSegments = segments
			.subList(partIndex, segments.lastIndex)
			.asReversed()
			.toList()

		val newPathPoints = mutableListOf(
			ScoochwormPathPoint(newHeadPosition, newSupportDirection)
		)

		for (segmentIndex in segments.lastIndex - 1 downTo partIndex) {
			val bodyPart = segments[segmentIndex].bodyPart ?: return null
			val newPoint = ScoochwormPathPoint(bodyPart.position(), bodyPart.supportDirection)
			newPathPoints.add(newPoint)
		}

		val newScoochworm = ScoochwormEntity.spawnFromSplit(
			source = scoochworm,
			headPosition = newHeadPosition,
			supportDirection = newSupportDirection,
			yaw = newYaw,
			segments = transferredSegments,
			pathPoints = newPathPoints
		)

		previousTail.dropAttachmentItem(scoochworm)
		previousTail.discardBodyPart()
		segments.subList(partIndex, segments.size).clear()

		return newScoochworm
	}

	fun replaceWith(newSegments: List<ScoochwormSegment>) {
		discard()
		segments.clear()
		segments.addAll(newSegments)
		for (segment in segments) {
			segment.assignBody(this)
		}

		for (partIndex in segments.indices) {
			segments[partIndex].reparentBodyPart(scoochworm, partIndex)
		}
	}

	fun interact(
		player: Player,
		hand: InteractionHand,
		partIndex: Int
	): InteractionResult {
		val heldStack = player.getItemInHand(hand)

		if (scoochworm.isClientSide) {
			val segment = getSegment(partIndex) ?: return InteractionResult.PASS
			return segment.predictInteraction(player, heldStack)
		}

		val segment = getSegment(partIndex) ?: return InteractionResult.PASS
		return segment.interact(
			player,
			hand,
			heldStack,
			onSheared = { splitAt(partIndex) }
		)
	}

	fun dropAllAttachmentItems() {
		for (segment in segments) {
			segment.dropAttachmentItem(scoochworm)
		}
	}

	fun insertIntoLockboxes(itemStack: ItemStack): ItemStack {
		var remainder = itemStack

		for (segment in segments) {
			remainder = segment.insertIntoLockbox(remainder)
			if (remainder.isEmpty) break
		}

		return remainder
	}

	fun tick() {
		if (scoochworm.isClientSide) return

		for (segment in segments) {
			segment.serverTick()
		}
	}

	fun getSegment(partIndex: Int): ScoochwormSegment? {
		return segments.getOrNull(partIndex)
	}

	fun hasAttachment(attachmentType: ScoochwormAttachmentType<*>): Boolean {
		return segments.any { it.getAttachment().type == attachmentType }
	}

	fun getBodyPart(partIndex: Int): ScoochwormPartEntity? {
		return getSegment(partIndex)?.bodyPart
	}

	fun bindClientBodyPart(bodyPart: ScoochwormPartEntity): ScoochwormSegment? {
		val partIndex = bodyPart.partIndex
		if (partIndex !in 0 until MAX_COUNT) return null

		while (segments.size <= partIndex) {
			val segment = ScoochwormSegment()
			segment.assignBody(this)
			segments.add(segment)
		}

		val targetSegment = segments[partIndex]
		for (segment in segments) {
			if (segment === targetSegment) continue
			segment.unbindClientBodyPart(bodyPart)
		}

		targetSegment.bindClientBodyPart(bodyPart, bodyPart.attachmentData)
		return targetSegment
	}

	fun updateColor(color: WormColor) {
		for (segment in segments) {
			val bodyPart = segment.bodyPart ?: continue
			bodyPart.color = color
		}
	}

	fun update(path: ScoochwormPath) {
		// Each segment samples the same head path at a progressively larger distance.
		for (partIndex in segments.indices) {
			val segment = segments[partIndex]
			val pathPoint = path.getPoint(getPartDistance(partIndex)) ?: continue
			segment.updateBodyPart(scoochworm, partIndex, pathPoint)
		}
	}

	fun save(): ListTag {
		val tag = ListTag()

		for (segment in segments) {
			tag.add(segment.save())
		}

		return tag
	}

	fun load(tag: ListTag) {
		discard()
		segments.clear()

		val segmentCount = tag.size.coerceIn(MIN_COUNT, MAX_COUNT)
		for (index in 0 until segmentCount) {
			segments.add(
				if (index < tag.size) {
					ScoochwormSegment.load(tag.getCompound(index))
				} else {
					ScoochwormSegment().also { it.assignBody(this) }
				}
			)
		}
	}

	fun discard() {
		for (segment in segments) {
			segment.discardBodyPart()
		}
	}

	private fun getPartDistance(partIndex: Int): Double {
		return ScoochwormEntity.PART_SPACING * (partIndex + 1)
	}

	companion object {
		private const val MIN_COUNT = 1
		const val MAX_COUNT = 16
	}
}