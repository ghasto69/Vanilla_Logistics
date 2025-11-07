package com.ghasto.logistical_improvements.mixin.cog_material;

import com.ghasto.logistical_improvements.cog_material.CogMaterial;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.kinetics.simpleRelays.BracketedKineticBlockEntity;
import com.simibubi.create.content.kinetics.simpleRelays.BracketedKineticBlockEntityRenderer;
import net.createmod.catnip.render.SuperByteBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(BracketedKineticBlockEntityRenderer.class)
public class BracketedKineticBERMixin {
    @ModifyArg(
            method = "renderSafe(Lcom/simibubi/create/content/kinetics/simpleRelays/BracketedKineticBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
            at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/kinetics/simpleRelays/BracketedKineticBlockEntityRenderer;renderRotatingBuffer(Lcom/simibubi/create/content/kinetics/base/KineticBlockEntity;Lnet/createmod/catnip/render/SuperByteBuffer;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;I)V"),
            index = 1
    )
    private SuperByteBuffer replaceLargeCogwheel(SuperByteBuffer original, @Local(argsOnly = true) BracketedKineticBlockEntity blockEntity) {
        return CogMaterial.createBuffer(true, false, blockEntity);
    }
}
