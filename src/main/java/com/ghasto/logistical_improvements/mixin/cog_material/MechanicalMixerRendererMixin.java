package com.ghasto.logistical_improvements.mixin.cog_material;

import com.ghasto.logistical_improvements.cog_material.CogMaterial;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.kinetics.mixer.MechanicalMixerBlockEntity;
import com.simibubi.create.content.kinetics.mixer.MechanicalMixerRenderer;
import net.createmod.catnip.render.SuperByteBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(MechanicalMixerRenderer.class)
public class MechanicalMixerRendererMixin {
    @ModifyArg(
            method = "renderSafe(Lcom/simibubi/create/content/kinetics/mixer/MechanicalMixerBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/simibubi/create/content/kinetics/mixer/MechanicalMixerRenderer;standardKineticRotationTransform(Lnet/createmod/catnip/render/SuperByteBuffer;Lcom/simibubi/create/content/kinetics/base/KineticBlockEntity;I)Lnet/createmod/catnip/render/SuperByteBuffer;"
            )
    )
    private SuperByteBuffer renderMaterialCog(SuperByteBuffer original, @Local(argsOnly = true) MechanicalMixerBlockEntity blockEntity) {
        return CogMaterial.createBuffer(false, false, blockEntity);
    }
}
