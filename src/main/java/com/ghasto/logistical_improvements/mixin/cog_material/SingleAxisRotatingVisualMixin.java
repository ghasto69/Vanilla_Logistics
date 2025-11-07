package com.ghasto.logistical_improvements.mixin.cog_material;

import com.ghasto.logistical_improvements.VanillaLogistics;
import com.ghasto.logistical_improvements.cog_material.CogMaterial;
import com.ghasto.logistical_improvements.cog_material.CogMaterialAccessor;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;
import com.simibubi.create.content.kinetics.simpleRelays.BracketedKineticBlockEntityVisual;
import com.simibubi.create.foundation.render.AllInstanceTypes;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SingleAxisRotatingVisual.class)
public abstract class SingleAxisRotatingVisualMixin<T extends KineticBlockEntity> extends KineticBlockEntityVisual<T> {
    @Shadow
    @Final
    @Mutable
    protected RotatingInstance rotatingModel;

    @Shadow public abstract void update(float pt);

    @Shadow public abstract void updateLight(float partialTick);

    @Unique
    private BlockState lastMaterial = Blocks.SPRUCE_PLANKS.defaultBlockState();

    public SingleAxisRotatingVisualMixin(VisualizationContext context, T blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
    }

    @Inject(
            method = "<init>(Ldev/engine_room/flywheel/api/visualization/VisualizationContext;Lcom/simibubi/create/content/kinetics/base/KineticBlockEntity;FLnet/minecraft/core/Direction;Ldev/engine_room/flywheel/api/model/Model;)V",
            at = @At("TAIL")
    )
    private void initMaterials(VisualizationContext context, KineticBlockEntity blockEntity, float partialTick, Direction from, Model model, CallbackInfo ci) {
        if(shouldReplace()) {
            updateMaterials(partialTick);
        }
    }

    @Inject(
            method = "update",
            at = @At("TAIL")
    )
    private void wrapUpdate(float pt, CallbackInfo ci) {
        if (shouldReplace()) {
            updateMaterials(pt);
        }
    }

    @Unique
    private void updateMaterials(float pt) {
        var material = ((CogMaterialAccessor) blockEntity).getMaterial();
        if (lastMaterial != material) {
            rotatingModel.delete();
            this.lastMaterial = material;
            rotatingModel = instancerProvider().instancer(AllInstanceTypes.ROTATING, CogMaterial.MODEL_CACHE.get(new CogMaterial.ModelKey((Object) this instanceof BracketedKineticBlockEntityVisual.LargeCogVisual, shouldHaveShaft(), blockState, material)))
                    .createInstance()
                    .rotateToFace(rotationAxis())
                    .setup(blockEntity)
                    .setPosition(getVisualPosition());
            updateLight(pt);
        }
    }

    @Unique
    private boolean shouldReplace() {
        return blockState.is(TagKey.create(Registries.BLOCK, VanillaLogistics.asId("material_cogs")));
    }

    @Unique
    private boolean shouldHaveShaft() {
        if(blockState.is(AllBlocks.COGWHEEL))
            return true;
        return blockState.is(AllBlocks.LARGE_COGWHEEL);
    }
}
