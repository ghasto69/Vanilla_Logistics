package com.ghasto.logistical_improvements;

import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlockEntity;
import com.simibubi.create.foundation.utility.AdventureUtil;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.LoggerFactory;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(LogisticalImprovements.MODID)
public class LogisticalImprovements {
    public static final String MODID = "vanilla_logistics";
    public static final Logger LOGGER = LoggerFactory.getLogger("Vanilla Logistics");

    public LogisticalImprovements(IEventBus eventBus, ModContainer modContainer) {
        eventBus.addListener(this::commonSetup);
        eventBus.addListener(this::registerPackets);
    }

    private void registerPackets(RegisterPayloadHandlersEvent event) {
        var registry = event.registrar("2"); // no idea what the 2 is for
        registry.playToServer(ConfigureBatchSize.TYPE, ConfigureBatchSize.STREAM_CODEC, (payload, context) -> {
            var player = context.player();
            var pos = payload.position();

            var level = player.level();
            if (!level.isLoaded(pos.pos()))
                return;

            if (!(level.getBlockEntity(pos.pos()) instanceof FactoryPanelBlockEntity be))
                return;

            var behavior = be.panels.get(pos.slot());
            if (!(behavior instanceof BatchSizeAccessor accessor))
                return;

            if (accessor.getMinimumBatchSize() == payload.value())
                return;

            accessor.setMinimumBatchSize(payload.value());
            be.notifyUpdate();
        });
    }

    private void commonSetup(FMLCommonSetupEvent event) {

    }
}
