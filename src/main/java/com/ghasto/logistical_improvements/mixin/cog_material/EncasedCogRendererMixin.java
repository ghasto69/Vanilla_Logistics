package com.ghasto.logistical_improvements.mixin.cog_material;

import com.ghasto.logistical_improvements.cog_material.CogMaterial;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.simibubi.create.content.kinetics.simpleRelays.SimpleKineticBlockEntity;
import com.simibubi.create.content.kinetics.simpleRelays.encased.EncasedCogRenderer;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EncasedCogRenderer.class)
public class EncasedCogRendererMixin {
    @Shadow private boolean large;

    @ModifyReturnValue(
            method = "getRotatedModel(Lcom/simibubi/create/content/kinetics/simpleRelays/SimpleKineticBlockEntity;Lnet/minecraft/world/level/block/state/BlockState;)Lnet/createmod/catnip/render/SuperByteBuffer;",
            at = @At("RETURN")
    )
    private SuperByteBuffer modelReturnMaterialAAAAA(SuperByteBuffer original, SimpleKineticBlockEntity be, BlockState state) {
        return CogMaterial.createBuffer(large, false, be);
    }
}
