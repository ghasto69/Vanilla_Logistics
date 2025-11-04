package com.ghasto.logistical_improvements.cog_material;

import com.ghasto.logistical_improvements.VanillaLogisticsClient;
import com.ghasto.logistical_improvements.mixin.cog_material.WaterWheelRendererAccessor;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.simpleRelays.CogWheelBlock;
import com.simibubi.create.foundation.model.BakedModelHelper;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.lib.model.baked.BakedModelBuilder;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.util.RendererReloadCache;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.createmod.catnip.registry.RegisteredObjectsHelper;
import net.createmod.catnip.render.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.Map;

public class CogMaterial {
    public static final SuperByteBufferCache.Compartment<ModelKey> COMPARTMENT = new SuperByteBufferCache.Compartment<>();
    public static final RendererReloadCache<ModelKey, Model> MODEL_CACHE = new RendererReloadCache<>(CogMaterial::createModel);

    private static final StitchedSprite SPRUCE_PLANKS = new StitchedSprite(ResourceLocation.withDefaultNamespace("block/spruce_planks"));
    private static final StitchedSprite STRIPPED_SPRUCE_LOG = new StitchedSprite(ResourceLocation.withDefaultNamespace("block/stripped_spruce_log_top"));

    public static SuperByteBuffer createBuffer(boolean large, boolean shaft, KineticBlockEntity blockEntity) {
        ModelKey key = new ModelKey(large, shaft, blockEntity.getBlockState(), ((CogMaterialAccessor) blockEntity).getMaterial());
        return SuperByteBufferCache.getInstance().get(CogMaterial.COMPARTMENT, key, () -> {
            var model = CogMaterial.generateModel(key.getModel().get(), key.material());
            var blockState = key.state();
            boolean hasDirection = blockState.hasProperty(BlockStateProperties.AXIS);
            var dir = Direction.fromAxisAndDirection(hasDirection ? blockState.getValue(BlockStateProperties.AXIS) : Direction.Axis.Y, Direction.AxisDirection.POSITIVE);
            var transform = CachedBuffers.rotateToFaceVertical(dir).get();

            return SuperBufferFactory.getInstance().createForBlock(model, Blocks.AIR.defaultBlockState(), transform);
        });
    }

    public static BakedModel generateModel(BakedModel template, BlockState planks) {
        var planksBlock = planks.getBlock();
        var id = RegisteredObjectsHelper.getKeyOrThrow(planksBlock);
        var wood = WaterWheelRendererAccessor.invokePlankStateToWoodName(planks);
        var strippedLog = WaterWheelRendererAccessor.invokeGetLogBlockstate(id.getNamespace(), "stripped_" + wood);

        var planksTexture = WaterWheelRendererAccessor.invokeGetSpriteOnSide(planks, Direction.SOUTH);
        var logTexture = WaterWheelRendererAccessor.invokeGetSpriteOnSide(strippedLog, Direction.UP);

        Map<TextureAtlasSprite, TextureAtlasSprite> map = new Reference2ReferenceOpenHashMap<>();

        map.put(SPRUCE_PLANKS.get(), planksTexture);
        map.put(STRIPPED_SPRUCE_LOG.get(), logTexture);

        return BakedModelHelper.generateModel(template, map::get);
    }

    public static Model createModel(ModelKey key) {
        BakedModel model = generateModel(key.getModel().get(), key.material());
        return new BakedModelBuilder(model)
                .build();
    }

    public record ModelKey(boolean large, boolean shaft, BlockState state, BlockState material) {
        public PartialModel getModel() {
            // use create cogwheel for spruce
            if(material == Blocks.SPRUCE_PLANKS.defaultBlockState())
                return large ? AllPartialModels.SHAFTLESS_LARGE_COGWHEEL : shaft ? AllPartialModels.COGWHEEL : AllPartialModels.SHAFTLESS_COGWHEEL;
            return large ? VanillaLogisticsClient.LARGE_COGWHEEL_SHAFTLESS : shaft ? VanillaLogisticsClient.COGWHEEL : VanillaLogisticsClient.COGWHEEL_SHAFTLESS;
        }
    }
}
