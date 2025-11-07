package com.ghasto.logistical_improvements.cog_material;

import net.minecraft.world.level.block.state.BlockState;

public interface CogMaterialAccessor {
    BlockState getMaterial();
    void setMaterial(BlockState material);
}
