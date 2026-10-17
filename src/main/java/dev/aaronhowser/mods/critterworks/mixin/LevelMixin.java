package dev.aaronhowser.mods.critterworks.mixin;

import dev.aaronhowser.mods.critterworks.ItemFilterFeederCarrier;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Level.class)
public abstract class LevelMixin implements ItemFilterFeederCarrier {

	@Unique
	final LongOpenHashSet critterworks$itemFilterFeederPositions = new LongOpenHashSet();

	@Unique
	@Override
	public LongOpenHashSet critterworks$getItemFilterFeederPositions() {
		return critterworks$itemFilterFeederPositions;
	}
}