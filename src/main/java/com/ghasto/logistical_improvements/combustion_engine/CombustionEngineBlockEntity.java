package com.ghasto.logistical_improvements.combustion_engine;

import com.ghasto.logistical_improvements.VLBlockEntities;
import com.ghasto.logistical_improvements.VLBlocks;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.content.kinetics.belt.behaviour.DirectBeltInputBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
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
        inv = new ItemStackHandler(1);
        capability = new CombustionEngineInventoryHandler();
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                VLBlockEntities.COMBUSTION_ENGINE.get(),
                (be, context) -> be.inv
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

        var stack = capability.getStackInSlot(0);
        if (fuelTicksRemaining <= 5 && !stack.isEmpty()) {
            int burnTime = stack.getBurnTime(RecipeType.SMELTING);
            int previous = fuelTicksRemaining;
            fuelTicksRemaining += burnTime;
            stack.shrink(1);
            notifyUpdate();

            if(previous == 0) {
                updateGeneratedRotation();
            }
            return;
        }

        if (!fueled()) return;

        fuelTicksRemaining--;
        notifyUpdate();
        if (!fueled()) {
            updateGeneratedRotation();
        }
    }

    @Override
    public void initialize() {
        super.initialize();
        if (!hasSource() || getGeneratedSpeed() > getTheoreticalSpeed())
            updateGeneratedRotation();
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

    public class CombustionEngineInventoryHandler extends CombinedInvWrapper {
        public CombustionEngineInventoryHandler() {
            super(inv);
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (!canInsert(stack)) return ItemStack.EMPTY;
            return super.insertItem(slot, stack, simulate);
        }
    }

    public static boolean canInsert(ItemStack stack) {
        return stack.getBurnTime(RecipeType.SMELTING) > 0 && !(stack.getItem() instanceof BucketItem);
    }
}
