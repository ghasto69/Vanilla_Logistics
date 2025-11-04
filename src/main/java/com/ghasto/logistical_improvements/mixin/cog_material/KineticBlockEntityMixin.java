package com.ghasto.logistical_improvements.mixin.cog_material;

import com.ghasto.logistical_improvements.cog_material.CogMaterialAccessor;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KineticBlockEntity.class)
public abstract class KineticBlockEntityMixin extends SmartBlockEntity implements CogMaterialAccessor {
    @Unique
    private BlockState material = Blocks.SPRUCE_PLANKS.defaultBlockState();

    public KineticBlockEntityMixin(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public BlockState getMaterial() {
        return this.material;
    }

    @Override
    public void setMaterial(BlockState material) {
        this.material = material;
        this.notifyUpdate();
    }

    @Inject(method = "write", at = @At("TAIL"))
    private void injectWrite(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket, CallbackInfo ci) {
        tag.put("cog_material", NbtUtils.writeBlockState(this.material));
    }

    @Inject(method = "read", at = @At("TAIL"))
    private void injectRead(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket, CallbackInfo ci) {
        this.material = NbtUtils.readBlockState(registries.lookupOrThrow(Registries.BLOCK), compound.getCompound("cog_material"));
    }

    @Inject(method = "switchToBlockState", at = @At("TAIL"))
    private static void keepMaterial(Level world, BlockPos pos, BlockState state, CallbackInfo ci, @Local BlockEntity blockEntity) {
        var newBlockEntity = world.getBlockEntity(pos);
        if(newBlockEntity instanceof CogMaterialAccessor accessor && blockEntity instanceof CogMaterialAccessor oldAccessor) {
            accessor.setMaterial(oldAccessor.getMaterial());
        }
    }
}
