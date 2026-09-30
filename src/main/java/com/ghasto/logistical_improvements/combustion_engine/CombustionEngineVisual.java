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

import java.util.Arrays;
import java.util.function.Consumer;

public class CombustionEngineVisual extends ShaftVisual<CombustionEngineBlockEntity> implements SimpleDynamicVisual {
    private final TransformedInstance[] pistons;

    public CombustionEngineVisual(VisualizationContext context, CombustionEngineBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);

        var instancer = instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(VanillaLogisticsClient.PISTON));
        pistons = new TransformedInstance[CombustionEngineRenderer.PISTONS];
        instancer.createInstances(pistons);
        relight(pistons);
    }

    private void animate() {
        var axis = blockState.getValue(CombustionEngineBlock.HORIZONTAL_AXIS);
        float renderTime = AnimationTickHolder.getRenderTime(level);

        for (int i = 0; i < pistons.length; i++) {
            TransformedInstance piston = pistons[i];
            piston.setIdentityTransform().translate(getVisualPosition());
            CombustionEngineRenderer.transformPiston(piston, i, axis, blockEntity.fueled(), renderTime)
                    .setChanged();
        }
    }

    @Override
    public void beginFrame(DynamicVisual.Context ctx) {
        animate();
    }

    @Override
    public void updateLight(float partialTick) {
        super.updateLight(partialTick);
        relight(pistons);
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
