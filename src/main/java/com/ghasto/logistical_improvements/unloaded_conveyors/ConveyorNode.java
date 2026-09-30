package com.ghasto.logistical_improvements.unloaded_conveyors;

import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

public class ConveyorNode {
    public final BlockPos pos;

    // The conveyor in the world, present for as long as its chunk is loaded
    @Nullable
    ChainConveyorBlockEntity loaded;
    // Stand-in that is not part of the world, carries the conveyor's state while its chunk is unloaded
    @Nullable
    ChainConveyorBlockEntity proxy;

    // Game time at which the speed of this conveyor was last confirmed by a loaded conveyor
    long speedStamp;
    int syncedPackages;

    ConveyorNode(BlockPos pos) {
        this.pos = pos.immutable();
    }

    @Nullable
    public ChainConveyorBlockEntity getConveyor() {
        if (loaded != null && !loaded.isRemoved())
            return loaded;
        return proxy;
    }

    public boolean isLoaded() {
        return loaded != null && !loaded.isRemoved();
    }
}
