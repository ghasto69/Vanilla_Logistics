package com.ghasto.logistical_improvements.mixin.cog_material;

import com.ghasto.logistical_improvements.cog_material.CogMaterial;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(KineticBlockEntityRenderer.class)
public class KineticBERMixin {
    @ModifyReturnValue(method = "getRotatedModel", at = @At("RETURN"))
    private SuperByteBuffer renderCogwheel(SuperByteBuffer original, KineticBlockEntity be, BlockState blockState) {
        if(blockState.is(AllBlocks.COGWHEEL))
            return CogMaterial.createBuffer(false, true, be);
        return original;
    }
}
