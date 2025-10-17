package com.ghasto.logistical_improvements.mixin;

import com.ghasto.logistical_improvements.BatchSizeAccessor;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlockEntity;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.joml.Math;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FactoryPanelBehaviour.class)
public abstract class FactoryPanelBehaviourMixin implements BatchSizeAccessor {
    @Shadow
    public FactoryPanelBlock.PanelSlot slot;

    @Shadow public abstract FactoryPanelBlockEntity panelBE();

    private int minimumBatchSize = 1;

    @Override
    public int getMinimumBatchSize() {
        return this.minimumBatchSize;
    }

    @Override
    public void setMinimumBatchSize(int minimumBatchSize) {
        this.minimumBatchSize = minimumBatchSize;
    }

    @Unique
    private void addTag(CompoundTag tag) {
        tag.putInt(slot.name() + "_minimumBatchSize", this.minimumBatchSize);
    }

    @Inject(method = "writeSafe", at = @At("TAIL"))
    private void writeSafe(CompoundTag nbt, HolderLookup.Provider registries, CallbackInfo ci) {
        addTag(nbt);
    }

    @Inject(method = "write", at = @At("TAIL"))
    private void write(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket, CallbackInfo ci) {
        addTag(nbt);
    }

    @Inject(method = "read", at = @At("TAIL"))
    private void read(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket, CallbackInfo ci) {
        this.minimumBatchSize = nbt.getInt(slot.name() + "_minimumBatchSize");
    }

    @Inject(
            method = "tryRestock",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/simibubi/create/content/logistics/stockTicker/PackageOrderWithCrafts;simple(Ljava/util/List;)Lcom/simibubi/create/content/logistics/stockTicker/PackageOrderWithCrafts;"
            ),
            cancellable = true
    )
    private void onTryRestock(CallbackInfo ci, @Local(ordinal = 5) int amountToOrder, @Local(ordinal = 0) int availableOnNetwork) {
        if (Math.min(amountToOrder, availableOnNetwork) < this.minimumBatchSize) {
            ci.cancel();
        }
    }
}
