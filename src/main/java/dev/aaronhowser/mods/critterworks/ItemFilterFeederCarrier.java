package dev.aaronhowser.mods.critterworks;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

public interface ItemFilterFeederCarrier {

	default LongOpenHashSet critterworks$getItemFilterFeederPositions() {
		throw new IllegalStateException();
	}

}