package com.ghasto.logistical_improvements.cog_material;

import com.simibubi.create.content.kinetics.waterwheel.WaterWheelRenderer;
import net.createmod.catnip.render.SuperByteBufferCache;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public interface CogMaterialAccessor {
    BlockState getMaterial();
    void setMaterial(BlockState material);
}
