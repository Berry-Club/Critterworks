package dev.aaronhowser.mods.critterworks.registry

import dev.aaronhowser.mods.aaron.misc.AaronExtensions.isItem
import dev.aaronhowser.mods.critterworks.Critterworks
import dev.aaronhowser.mods.critterworks.datagen.tag.ModItemTagsProvider
import dev.aaronhowser.mods.critterworks.entity.attachment.ScoochwormAttachmentType
import dev.aaronhowser.mods.critterworks.entity.attachment.builtin.LockboxAttachment
import dev.aaronhowser.mods.critterworks.entity.attachment.builtin.NoAttachment
import dev.aaronhowser.mods.critterworks.entity.attachment.builtin.SaddleAttachment
import dev.aaronhowser.mods.critterworks.entity.attachment.data.LockboxAttachmentData
import dev.aaronhowser.mods.critterworks.entity.attachment.data.NoAttachmentData
import dev.aaronhowser.mods.critterworks.entity.attachment.data.SaddleAttachmentData
import dev.aaronhowser.mods.critterworks.entity.attachment.data.SyncedAttachmentData
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceKey
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister
import net.minecraft.world.item.ItemStack
import java.util.function.Supplier

object ModScoochwormAttachmentTypes {

	val REGISTRY_KEY: ResourceKey<Registry<ScoochwormAttachmentType<*>>> = ResourceKey.createRegistryKey(
		Critterworks.modResource("scoochworm_attachment_type")
	)

	val SCOOCHWORM_ATTACHMENT_TYPE_REGISTRY: DeferredRegister<ScoochwormAttachmentType<*>> =
		DeferredRegister.create(REGISTRY_KEY, Critterworks.MOD_ID)

	val REGISTRY: Registry<ScoochwormAttachmentType<*>> = SCOOCHWORM_ATTACHMENT_TYPE_REGISTRY.makeRegistry {
		it.sync(true)
	}

	val NONE: DeferredHolder<ScoochwormAttachmentType<*>, ScoochwormAttachmentType<NoAttachmentData>> =
		register("none") {
			ScoochwormAttachmentType(
				streamCodec = NoAttachmentData.STREAM_CODEC,
				matchesItem = { false },
				createFromItem = { NoAttachment() },
				createEmpty = ::NoAttachment
			)
		}

	val LOCKBOX: DeferredHolder<ScoochwormAttachmentType<*>, ScoochwormAttachmentType<LockboxAttachmentData>> =
		register("lockbox") {
			ScoochwormAttachmentType(
				streamCodec = LockboxAttachmentData.STREAM_CODEC,
				matchesItem = { itemStack -> itemStack.isItem(ModItems.LOCKBOX) },
				createFromItem = ::LockboxAttachment,
				createEmpty = { LockboxAttachment(ItemStack.EMPTY) }
			)
		}

	val SADDLE: DeferredHolder<ScoochwormAttachmentType<*>, ScoochwormAttachmentType<SaddleAttachmentData>> =
		register("saddle") {
			ScoochwormAttachmentType(
				streamCodec = SaddleAttachmentData.STREAM_CODEC,
				matchesItem = { itemStack ->
					itemStack.isItem(ModItemTagsProvider.SCOOCHWORM_SADDLES)
				},
				createFromItem = ::SaddleAttachment,
				createEmpty = { SaddleAttachment(ItemStack.EMPTY) }
			)
		}

	private fun <T : SyncedAttachmentData> register(
		name: String,
		supplier: Supplier<ScoochwormAttachmentType<T>>
	): DeferredHolder<ScoochwormAttachmentType<*>, ScoochwormAttachmentType<T>> {
		return SCOOCHWORM_ATTACHMENT_TYPE_REGISTRY.register(name, supplier)
	}
}
