package dev.aaronhowser.mods.critterworks.registry

import dev.aaronhowser.mods.critterworks.Critterworks
import net.minecraft.network.codec.ByteBufCodecs
import net.neoforged.neoforge.attachment.AttachmentType
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister
import net.neoforged.neoforge.registries.NeoForgeRegistries
import java.util.function.Supplier

object ModAttachmentTypes {

	val ATTACHMENT_TYPE_REGISTRY: DeferredRegister<AttachmentType<*>> =
		DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Critterworks.MOD_ID)

	val IS_SHELL_ACTIVE: DeferredHolder<AttachmentType<*>, AttachmentType<Boolean>> =
		register(
			"is_shell_active",
			AttachmentType
				.builder(Supplier { false })
				.sync(ByteBufCodecs.BOOL)
				.build()
		)

	private fun <T> register(name: String, type: AttachmentType<T>): DeferredHolder<AttachmentType<*>, AttachmentType<T>> {
		return ATTACHMENT_TYPE_REGISTRY.register(name, Supplier { type })
	}

}