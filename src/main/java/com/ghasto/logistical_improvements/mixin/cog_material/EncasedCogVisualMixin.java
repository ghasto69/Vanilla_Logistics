package com.ghasto.logistical_improvements.mixin.cog_material;

import com.ghasto.logistical_improvements.cog_material.CogMaterial;
import com.ghasto.logistical_improvements.cog_material.CogMaterialAccessor;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.content.kinetics.simpleRelays.encased.EncasedCogVisual;
import com.simibubi.create.foundation.render.AllInstanceTypes;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EncasedCogVisual.class)
public abstract class EncasedCogVisualMixin extends KineticBlockEntityVisual<KineticBlockEntity> {
    @Shadow @Final @Mutable protected RotatingInstance rotatingModel;
    @Shadow @Final private boolean large;
    @Unique
    private BlockState lastMaterial = Blocks.SPRUCE_PLANKS.defaultBlockState();

    public EncasedCogVisualMixin(VisualizationContext context, KineticBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
    }

    @Inject(
            method = "<init>",
            at = @At("TAIL")
    )
    private void injectInit(VisualizationContext modelManager, KineticBlockEntity blockEntity, boolean large, float partialTick, Model model, CallbackInfo ci) {
        updateMaterial(partialTick);
    }

    @Inject(
            method = "update",
            at = @At("TAIL")
    )
    private void injectUpdate(float pt, CallbackInfo ci) {
        updateMaterial(pt);
    }

    @Unique
    private void updateMaterial(float partialTick) {
        var material = ((CogMaterialAccessor) blockEntity).getMaterial();
        if (lastMaterial != material) {
            rotatingModel.delete();
            this.lastMaterial = material;
            rotatingModel = instancerProvider().instancer(AllInstanceTypes.ROTATING, CogMaterial.MODEL_CACHE.get(new CogMaterial.ModelKey(large, false, blockState, material)))
                    .createInstance()
                    .rotateToFace(rotationAxis())
                    .setup(blockEntity)
                    .setPosition(getVisualPosition());
            updateLight(partialTick);
        }
    }
}
