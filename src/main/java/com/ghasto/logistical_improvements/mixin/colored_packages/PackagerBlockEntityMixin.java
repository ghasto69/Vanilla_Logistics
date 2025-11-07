package com.ghasto.logistical_improvements.mixin.colored_packages;

import com.ghasto.logistical_improvements.colored_packages.PackagerValueBoxBehaviour;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(PackagerBlockEntity.class)
public abstract class PackagerBlockEntityMixin extends SmartBlockEntity {
    private PackagerValueBoxBehaviour colorBox;

    public PackagerBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Inject(method = "addBehaviours", at = @At("TAIL"))
    private void tailAddBehaviours(List<BlockEntityBehaviour> behaviours, CallbackInfo ci) {
        /*colorBox = new PackagerValueBoxBehaviour(this);
        behaviours.add(colorBox);*/
    }
}
