package com.ghasto.logistical_improvements;

import com.ghasto.logistical_improvements.cog_material.CogMaterial;
import com.ghasto.logistical_improvements.combustion_engine.CombustionEngineVisual;
import dev.engine_room.flywheel.api.visualization.VisualizerRegistry;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
import net.createmod.catnip.render.SuperByteBufferCache;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

@Mod(value = VanillaLogistics.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = VanillaLogistics.MODID, value = Dist.CLIENT)
public class VanillaLogisticsClient {
    public static final PartialModel COGWHEEL = PartialModel.of(VanillaLogistics.asId("block/custom_cogwheel"));
    public static final PartialModel COGWHEEL_SHAFTLESS = PartialModel.of(VanillaLogistics.asId("block/custom_cogwheel_shaftless"));
    public static final PartialModel LARGE_COGWHEEL_SHAFTLESS = PartialModel.of(VanillaLogistics.asId("block/custom_large_cogwheel_shaftless"));
    public static final PartialModel PISTON = PartialModel.of(VanillaLogistics.asId("block/piston"));

    public VanillaLogisticsClient() {
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        SuperByteBufferCache.getInstance().registerCompartment(CogMaterial.COMPARTMENT);
        event.enqueueWork(() -> {
            var engine_visual = new SimpleBlockEntityVisualizer<>(CombustionEngineVisual::new, v -> false);
            VisualizerRegistry.setVisualizer(VLBlockEntities.COMBUSTION_ENGINE.get(), engine_visual);
        });
    }

    @SubscribeEvent
    static void colorHandlers(RegisterColorHandlersEvent.Item event) {
        //event.register((stack, tintIndex) -> 0x00FF00, PackageStyles.STANDARD_BOXES.getFirst());
    }
}
