package dev.aaronhowser.mods.critterworks.registry

import dev.aaronhowser.mods.critterworks.Critterworks
import dev.aaronhowser.mods.critterworks.entity.scoochworm.attachment.data.SyncedAttachmentData
import net.minecraft.network.syncher.EntityDataSerializer
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister
import net.neoforged.neoforge.registries.NeoForgeRegistries
import java.util.function.Supplier

object ModEntityDataSerializers {

	val ENTITY_DATA_SERIALIZER_REGISTRY: DeferredRegister<EntityDataSerializer<*>> =
		DeferredRegister.create(
			NeoForgeRegistries.ENTITY_DATA_SERIALIZERS,
			Critterworks.MOD_ID
		)

	val SCOOCHWORM_ATTACHMENT_DATA: DeferredHolder<EntityDataSerializer<*>, EntityDataSerializer<SyncedAttachmentData>> =
		register(
			"scoochworm_attachment_data",
			EntityDataSerializer.forValueType(
				SyncedAttachmentData.STREAM_CODEC
			)
		)

	private fun <T : EntityDataSerializer<*>> register(
		name: String,
		serializer: T
	): DeferredHolder<EntityDataSerializer<*>, T> {
		return ENTITY_DATA_SERIALIZER_REGISTRY.register(name, Supplier { serializer })
	}

}