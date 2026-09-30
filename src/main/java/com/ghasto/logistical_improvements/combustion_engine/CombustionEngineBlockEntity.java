package com.ghasto.logistical_improvements.combustion_engine;

import com.ghasto.logistical_improvements.VLBlockEntities;
import com.ghasto.logistical_improvements.VLBlocks;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.content.kinetics.belt.behaviour.DirectBeltInputBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.item.ItemHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.CombinedInvWrapper;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CombustionEngineBlockEntity extends GeneratingKineticBlockEntity {
    public ItemStackHandler inv;
    private int fuelTicksRemaining = 0;
    public CombustionEngineInventoryHandler capability;

    public CombustionEngineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        inv = new ItemStackHandler(1) {
            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                return canInsert(stack);
            }

            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }
        };
        capability = new CombustionEngineInventoryHandler();
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                VLBlockEntities.COMBUSTION_ENGINE.get(),
                (be, context) -> be.capability
        );
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        behaviours.add(new DirectBeltInputBehaviour(this));
    }

    @Override
    public float getGeneratedSpeed() {
        if (!fueled()) return 0;
        if (!VLBlocks.COMBUSTION_ENGINE.has(getBlockState()))
            return 0;
        return 64;
    }

    @Override
    public float calculateAddedStressCapacity() {
        if (!fueled()) return 0;
        return super.calculateAddedStressCapacity();
    }

    @Override
    public void tick() {
        super.tick();
        if(level.isClientSide()) return;

        boolean wasFueled = fueled();
        if (wasFueled)
            fuelTicksRemaining--;
        if (fuelTicksRemaining <= 5)
            refuel();

        // The remaining ticks are not synced, clients only care about whether the engine is running
        if (wasFueled != fueled()) {
            setChanged();
            updateGeneratedRotation();
        }
    }

    private void refuel() {
        var stack = inv.getStackInSlot(0);
        if (stack.isEmpty()) return;

        int burnTime = stack.getBurnTime(RecipeType.SMELTING);
        if (burnTime <= 0) {
            // Not a fuel (anymore), hand it back instead of jamming the engine
            inv.setStackInSlot(0, ItemStack.EMPTY);
            Block.popResource(level, worldPosition, stack);
            return;
        }

        fuelTicksRemaining += burnTime;
        inv.extractItem(0, 1, false);
    }

    @Override
    public void initialize() {
        super.initialize();
        if (!hasSource() || getGeneratedSpeed() > getTheoreticalSpeed())
            updateGeneratedRotation();
    }

    @Override
    public void destroy() {
        super.destroy();
        ItemHelper.dropContents(level, worldPosition, inv);
    }

    public boolean fueled() {
        return fuelTicksRemaining > 0;
    }

    @Override
    public void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putInt("engine_fuel_ticks", fuelTicksRemaining);
        tag.put("inventory", inv.serializeNBT(registries));
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        fuelTicksRemaining = tag.getInt("engine_fuel_ticks");
        inv.deserializeNBT(registries, tag.getCompound("inventory"));
    }

    /**
     * What other blocks get to see: fuel can be inserted, but not taken back out
     */
    public class CombustionEngineInventoryHandler extends CombinedInvWrapper {
        public CombustionEngineInventoryHandler() {
            super(inv);
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }
    }

    public static boolean canInsert(ItemStack stack) {
        return stack.getBurnTime(RecipeType.SMELTING) > 0 && !(stack.getItem() instanceof BucketItem);
    }
}
