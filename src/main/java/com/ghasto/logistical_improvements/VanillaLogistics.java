package com.ghasto.logistical_improvements;

import com.ghasto.logistical_improvements.batch_size.BatchSizeAccessor;
import com.ghasto.logistical_improvements.batch_size.ConfigureBatchSize;
import com.ghasto.logistical_improvements.cog_material.CogMaterialAccessor;
import com.ghasto.logistical_improvements.combustion_engine.CombustionEngineBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlockEntity;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.KineticStats;
import com.simibubi.create.foundation.item.TooltipModifier;
import net.createmod.catnip.lang.FontHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.LogicalSide;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(VanillaLogistics.MODID)
public class VanillaLogistics {
    public static final String MODID = "vanilla_logistics";
    public static final Logger LOGGER = LoggerFactory.getLogger("Vanilla Logistics");
    public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(MODID).setTooltipModifierFactory(item ->
            new ItemDescription.Modifier(item, FontHelper.Palette.STANDARD_CREATE)
                    .andThen(TooltipModifier.mapNull(KineticStats.create(item)))
    );

    public static final TagKey<Block> MATERIAL_COGS_TAG = TagKey.create(Registries.BLOCK, VanillaLogistics.asId("material_cogs"));

    public VanillaLogistics(IEventBus eventBus) {
        VLBlocks.init();
        VLBlockEntities.init();
        VLArmInteractionTypes.init();

        addAdditionalTranslations();

        eventBus.addListener(this::registerPackets);
        NeoForge.EVENT_BUS.addListener(this::useItemOnBlock);
        eventBus.addListener(CombustionEngineBlockEntity::registerCapabilities);
        REGISTRATE.registerEventListeners(eventBus);
    }

    public static ResourceLocation asId(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    private void addAdditionalTranslations() {
        var lang = new HashMap<String, String>();
        lang.put("tooltip.vanilla_logistics.minimum_batch_size", "Minimum Batch Size");
        lang.put("block.vanilla_logistics.combustion_engine.tooltip.summary", "A compact way to get some _stress_ by burning _fossil fuels_");
        lang.forEach(REGISTRATE::addRawLang);
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
        if (event.getHand() == InteractionHand.OFF_HAND) return;
        if (event.getUsePhase() != UseItemOnBlockEvent.UsePhase.BLOCK) return;
        var blockState = event.getLevel().getBlockState(event.getPos());
        if (!blockState.is(MATERIAL_COGS_TAG)) return;
        var stack = event.getItemStack();
        if (!stack.is(ItemTags.PLANKS)) return;
        var material = ((BlockItem) stack.getItem()).getBlock().defaultBlockState();
        var blockEntity = event.getLevel().getBlockEntity(event.getPos());
        if (!(blockEntity instanceof KineticBlockEntity)) return;
        var accessor = (CogMaterialAccessor) blockEntity;
        if (accessor.getMaterial() == material) return;
        if (event.getSide() == LogicalSide.SERVER) {
            accessor.setMaterial(material);
            event.getLevel().levelEvent(2001, event.getPos(), Block.getId(material)); //Block breaking particles
        }
        event.setCancellationResult(ItemInteractionResult.SUCCESS);
        event.setCanceled(true);
    }
}
