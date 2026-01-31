package com.ghasto.logistical_improvements.combustion_engine;

import com.ghasto.logistical_improvements.VanillaLogisticsClient;
import com.simibubi.create.content.kinetics.base.ShaftVisual;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.core.Direction;

import java.util.Arrays;
import java.util.function.Consumer;

public class CombustionEngineVisual extends ShaftVisual<CombustionEngineBlockEntity> implements SimpleDynamicVisual {
    private final TransformedInstance[] pistons;

    public CombustionEngineVisual(VisualizationContext context, CombustionEngineBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);

        var instancer = instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(VanillaLogisticsClient.PISTON));
        pistons = new TransformedInstance[6];
        instancer.createInstances(pistons);
        for (int i = 0; i < pistons.length; i++) {
            pistons[i].light(computePackedLight());
        }
    }

    private void animate() {
        for (int i = 0; i < pistons.length; i++) {
            final float px = 1/16f;
            TransformedInstance piston = pistons[i];
            piston.setIdentityTransform().translate(getVisualPosition());

            //Turn the block
            piston.translate(0.5f, 0, 0.5f);
            piston.rotateYDegrees(blockState.getValue(CombustionEngineBlock.HORIZONTAL_AXIS) == Direction.Axis.X ? 0 : 90);
            piston.translate(-0.5f, 0, -0.5f);

            piston.rotateYCenteredDegrees(90);

            int side = i < 3 ? 1 : -1;
            piston.translate(side * 7*px, px, 5*px);

            int third = i < 3 ? i : i - 3;
            piston.translate(0, 0, third * -5*px);

            piston.rotateZCenteredDegrees(side * -22.5f);

            if(blockEntity.fueled()) {
                double movement = Math.sin(AnimationTickHolder.getRenderTime(level) / 1.5) / 15;
                piston.translate(0, i % 2 == 0 ? movement : 1 / 15f - movement, 0);
            }

            piston.setChanged();
        }
    }

    @Override
    public void beginFrame(DynamicVisual.Context ctx) {
        animate();
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        super.collectCrumblingInstances(consumer);
        Arrays.stream(pistons).forEach(consumer);
    }

    @Override
    protected void _delete() {
        super._delete();
        Arrays.stream(pistons).forEach(TransformedInstance::delete);
    }
}
