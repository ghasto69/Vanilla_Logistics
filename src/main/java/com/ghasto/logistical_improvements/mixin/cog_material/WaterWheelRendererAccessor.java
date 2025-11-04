package com.ghasto.logistical_improvements.mixin.cog_material;

import com.simibubi.create.content.kinetics.waterwheel.WaterWheelRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(WaterWheelRenderer.class)
public interface WaterWheelRendererAccessor {
    @Invoker(value = "plankStateToWoodName")
    static String invokePlankStateToWoodName(BlockState blockState) {
        return null;
    }

    @Invoker(value = "getLogBlockState")
    static BlockState invokeGetLogBlockstate(String namespace, String wood) {
        return null;
    }

    @Invoker(value = "getSpriteOnSide")
    static TextureAtlasSprite invokeGetSpriteOnSide(BlockState state, Direction side) {
        return null;
    }
}
