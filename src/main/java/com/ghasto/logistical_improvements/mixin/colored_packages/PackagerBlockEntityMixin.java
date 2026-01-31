package com.ghasto.logistical_improvements.mixin.colored_packages;

import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.DyeColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PackagerBlockEntity.class)
public class PackagerBlockEntityMixin {
    @Unique
    private DyeColor packageColor = null;

    @Inject(method = "write", at = @At("TAIL"))
    private void tailWrite(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket, CallbackInfo ci) {
        if(packageColor == null) return;
        compound.putInt("package_color", packageColor.getId());
    }

    @Inject(method = "read", at = @At("TAIL"))
    private void tailRead(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket, CallbackInfo ci) {
        if(!compound.contains("package_color")) return;
        packageColor = DyeColor.byId(compound.getInt("package_color"));
    }
}
