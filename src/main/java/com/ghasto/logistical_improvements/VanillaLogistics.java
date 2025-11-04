package com.ghasto.logistical_improvements;

import com.ghasto.logistical_improvements.batch_size.BatchSizeAccessor;
import com.ghasto.logistical_improvements.batch_size.ConfigureBatchSize;
import com.ghasto.logistical_improvements.cog_material.CogMaterialAccessor;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.neoforged.fml.LogicalSide;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.slf4j.Logger;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import org.slf4j.LoggerFactory;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(VanillaLogistics.MODID)
public class VanillaLogistics {
    public static final String MODID = "vanilla_logistics";
    public static final Logger LOGGER = LoggerFactory.getLogger("Vanilla Logistics");

    public VanillaLogistics(IEventBus eventBus, ModContainer modContainer) {
        eventBus.addListener(this::registerPackets);
        NeoForge.EVENT_BUS.addListener(this::useItemOnBlock);
    }

    public static ResourceLocation asId(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
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

    private void useItemOnBlock(UseItemOnBlockEvent event) {
        if(event.getHand() == InteractionHand.OFF_HAND) return;
        if(event.getUsePhase() != UseItemOnBlockEvent.UsePhase.BLOCK) return;
        var blockState = event.getLevel().getBlockState(event.getPos());
        if(!blockState.is(TagKey.create(Registries.BLOCK, VanillaLogistics.asId("material_cogs")))) return;
        var stack = event.getItemStack();
        if(!stack.is(ItemTags.PLANKS)) return;
        var material = ((BlockItem) stack.getItem()).getBlock().defaultBlockState();
        var blockEntity = event.getLevel().getBlockEntity(event.getPos());
        if(!(blockEntity instanceof KineticBlockEntity)) return;
        var accessor = (CogMaterialAccessor) blockEntity;
        if(accessor.getMaterial() == material) return;
        if(event.getSide() == LogicalSide.SERVER) {
            accessor.setMaterial(material);
            event.getLevel().levelEvent(2001, event.getPos(), Block.getId(material));
        }
        event.setCancellationResult(ItemInteractionResult.SUCCESS);
        event.setCanceled(true);
    }
}
