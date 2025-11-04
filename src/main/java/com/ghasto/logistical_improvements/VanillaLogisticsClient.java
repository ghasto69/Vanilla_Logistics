package com.ghasto.logistical_improvements;

import com.ghasto.logistical_improvements.cog_material.CogMaterial;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.SuperByteBufferCache;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@Mod(value = VanillaLogistics.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = VanillaLogistics.MODID, value = Dist.CLIENT)
public class VanillaLogisticsClient {
    public static final PartialModel COGWHEEL = PartialModel.of(VanillaLogistics.asId("block/custom_cogwheel"));
    public static final PartialModel COGWHEEL_SHAFTLESS = PartialModel.of(VanillaLogistics.asId("block/custom_cogwheel_shaftless"));
    public static final PartialModel LARGE_COGWHEEL_SHAFTLESS = PartialModel.of(VanillaLogistics.asId("block/custom_large_cogwheel_shaftless"));

    public VanillaLogisticsClient(ModContainer container) {
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        SuperByteBufferCache.getInstance().registerCompartment(CogMaterial.COMPARTMENT);
    }
}
