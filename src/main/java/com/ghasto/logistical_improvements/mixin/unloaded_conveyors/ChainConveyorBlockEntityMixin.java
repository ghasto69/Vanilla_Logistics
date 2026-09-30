package com.ghasto.logistical_improvements.mixin.unloaded_conveyors;

import com.ghasto.logistical_improvements.unloaded_conveyors.ConveyorNetwork;
import com.ghasto.logistical_improvements.unloaded_conveyors.TrackedConveyor;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChainConveyorBlockEntity.class)
public abstract class ChainConveyorBlockEntityMixin extends KineticBlockEntity implements TrackedConveyor {
    @Unique
    private long lastServerTick = -1;
    @Unique
    private ConveyorNetwork conveyorNetwork;

    public ChainConveyorBlockEntityMixin(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public long getLastServerTick() {
        return this.lastServerTick;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        if (!(level instanceof ServerLevel serverLevel))
            return;
        this.lastServerTick = serverLevel.getGameTime();
        getConveyorNetwork(serverLevel).track((ChainConveyorBlockEntity) (Object) this);
    }

    @Inject(method = "addConnectionTo", at = @At("TAIL"))
    private void onConnect(BlockPos target, CallbackInfoReturnable<Boolean> cir) {
        if (level instanceof ServerLevel serverLevel)
            getConveyorNetwork(serverLevel).track((ChainConveyorBlockEntity) (Object) this);
    }

    @Inject(method = "remove", at = @At("HEAD"))
    private void onRemove(CallbackInfo ci) {
        if (level instanceof ServerLevel serverLevel)
            getConveyorNetwork(serverLevel).untrack((ChainConveyorBlockEntity) (Object) this);
    }

    // Connected conveyors in unloaded chunks are swapped for their stand-ins, rather than loading the chunk
    @WrapOperation(
            method = {"tick", "canAcceptPackagesFor"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;getBlockEntity(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/entity/BlockEntity;"
            )
    )
    private BlockEntity getConnectedConveyor(Level level, BlockPos pos, Operation<BlockEntity> original) {
        if (level instanceof ServerLevel serverLevel)
            return getConveyorNetwork(serverLevel).getConveyor(pos);
        return original.call(level, pos);
    }

    @WrapOperation(
            method = {"exportToPort", "notifyPortToAnticipate"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;getBlockEntity(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/entity/BlockEntity;"
            )
    )
    private BlockEntity getPortIfLoaded(Level level, BlockPos pos, Operation<BlockEntity> original) {
        if (level instanceof ServerLevel && !level.isLoaded(pos))
            return null;
        return original.call(level, pos);
    }

    @Unique
    private ConveyorNetwork getConveyorNetwork(ServerLevel level) {
        if (this.conveyorNetwork == null)
            this.conveyorNetwork = ConveyorNetwork.get(level);
        return this.conveyorNetwork;
    }
}
