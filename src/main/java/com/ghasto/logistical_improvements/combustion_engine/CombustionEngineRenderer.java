package com.ghasto.logistical_improvements.combustion_engine;

import com.ghasto.logistical_improvements.VanillaLogisticsClient;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.ShaftRenderer;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.transform.Affine;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;

/**
 * Only used when flywheel is off, see {@link CombustionEngineVisual}
 */
public class CombustionEngineRenderer extends ShaftRenderer<CombustionEngineBlockEntity> {
    public static final int PISTONS = 6;

    public CombustionEngineRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(CombustionEngineBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
                              int light, int overlay) {
        super.renderSafe(be, partialTicks, ms, buffer, light, overlay);
        if (VisualizationManager.supportsVisualization(be.getLevel())) return;

        var blockState = be.getBlockState();
        var axis = blockState.getValue(CombustionEngineBlock.HORIZONTAL_AXIS);
        var vertexBuilder = buffer.getBuffer(RenderType.solid());
        float renderTime = AnimationTickHolder.getRenderTime(be.getLevel());

        for (int i = 0; i < PISTONS; i++) {
            transformPiston(CachedBuffers.partial(VanillaLogisticsClient.PISTON, blockState), i, axis, be.fueled(), renderTime)
                    .light(light)
                    .renderInto(ms, vertexBuilder);
        }
    }

    public static <T extends Affine<T>> T transformPiston(T piston, int i, Direction.Axis axis, boolean fueled, float renderTime) {
        final float px = 1/16f;

        //Turn the block
        piston.translate(0.5f, 0, 0.5f);
        piston.rotateYDegrees(axis == Direction.Axis.X ? 0 : 90);
        piston.translate(-0.5f, 0, -0.5f);

        piston.rotateYCenteredDegrees(90);

        int side = i < 3 ? 1 : -1;
        piston.translate(side * 7*px, px, 5*px);

        int third = i < 3 ? i : i - 3;
        piston.translate(0, 0, third * -5*px);

        piston.rotateZCenteredDegrees(side * -22.5f);

        if(fueled) {
            double movement = Math.sin(renderTime / 1.5) / 15;
            piston.translate(0, i % 2 == 0 ? movement : 1 / 15f - movement, 0);
        }

        return piston;
    }
}
