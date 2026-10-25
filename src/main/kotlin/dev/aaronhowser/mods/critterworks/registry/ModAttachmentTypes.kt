package dev.aaronhowser.mods.critterworks.registry

import dev.aaronhowser.mods.critterworks.Critterworks
import dev.aaronhowser.mods.critterworks.item.SnailShieldItem
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.codec.ByteBufCodecs
import net.neoforged.neoforge.attachment.AttachmentType
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister
import net.neoforged.neoforge.registries.NeoForgeRegistries
import java.util.*
import java.util.function.Supplier

object ModAttachmentTypes {

	val ATTACHMENT_TYPE_REGISTRY: DeferredRegister<AttachmentType<*>> =
		DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Critterworks.MOD_ID)

	val SNAIL_SHELL_STATE: DeferredHolder<AttachmentType<*>, AttachmentType<SnailShieldItem.SnailShellState>> =
		register(
			"snail_shell_state",
			AttachmentType
				.builder(Supplier { SnailShieldItem.SnailShellState.UNSHELLED })
				.sync(SnailShieldItem.SnailShellState.STREAM_CODEC)
				.build()
		)

	val SNAIL_SHELL_STATIONARY_TICKS: DeferredHolder<AttachmentType<*>, AttachmentType<Int>> =
		register(
			"snail_shell_stationary_ticks",
			AttachmentType
				.builder(Supplier { 0 })
				.build()
		)

	val WORN_PUNCHPRAWN: DeferredHolder<AttachmentType<*>, AttachmentType<Optional<CompoundTag>>> =
		register(
			"worn_punchprawn",
			AttachmentType
				.builder(Supplier { Optional.empty<CompoundTag>() })
				.serialize(CompoundTag.CODEC.optionalFieldOf("entity").codec(), Optional<CompoundTag>::isPresent)
				.sync(ByteBufCodecs.OPTIONAL_COMPOUND_TAG)
				.copyOnDeath()
				.build()
		)

	val PUNCHPRAWN_COOLDOWN: DeferredHolder<AttachmentType<*>, AttachmentType<Int>> =
		register(
			"punchprawn_cooldown",
			AttachmentType
				.builder(Supplier { 0 })
				.sync(ByteBufCodecs.VAR_INT)
				.build()
		)

	private fun <T> register(name: String, type: AttachmentType<T>): DeferredHolder<AttachmentType<*>, AttachmentType<T>> {
		return ATTACHMENT_TYPE_REGISTRY.register(name, Supplier { type })
	}

}