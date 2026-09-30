package com.ghasto.logistical_improvements.combustion_engine;

import com.ghasto.logistical_improvements.VLBlockEntities;
import com.simibubi.create.content.kinetics.base.HorizontalAxisKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.math.VoxelShaper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.stream.Stream;

public class CombustionEngineBlock extends HorizontalAxisKineticBlock implements IBE<CombustionEngineBlockEntity> {
    public static final VoxelShape SHAPE = Shapes.join(Block.box(1, 0, 1, 15, 3, 15), Stream.of(
            Block.box(1, 0, 0, 15, 3, 1),
            Block.box(1, 3, 0, 15, 4, 1),
            Block.box(1, 0, 15, 15, 3, 16),
            Block.box(1, 3, 15, 15, 4, 16),
            Block.box(4, 3, 1, 12, 14, 15)
    ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get(), BooleanOp.OR);

    public CombustionEngineBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Class<CombustionEngineBlockEntity> getBlockEntityClass() {
        return CombustionEngineBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends CombustionEngineBlockEntity> getBlockEntityType() {
        return VLBlockEntities.COMBUSTION_ENGINE.get();
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(HORIZONTAL_AXIS);
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face.getAxis() == state.getValue(HORIZONTAL_AXIS);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return VoxelShaper.forHorizontalAxis(SHAPE, Direction.Axis.Z).get(state.getValue(HORIZONTAL_AXIS));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction.Axis preferredAxis = getPreferredHorizontalAxis(context);
        if (preferredAxis != null)
            return this.defaultBlockState().setValue(HORIZONTAL_AXIS, preferredAxis);
        return this.defaultBlockState().setValue(HORIZONTAL_AXIS, context.getHorizontalDirection().getAxis());
    }


    @Override
    public void updateEntityAfterFallOn(BlockGetter level, Entity entity) {
        super.updateEntityAfterFallOn(level, entity);
        if(entity.level().isClientSide()) return;

        CombustionEngineBlockEntity engine = null;
        for (BlockPos pos : Iterate.hereAndBelow(entity.blockPosition()))
            if (engine == null)
                engine = getBlockEntity(level, pos);

        if(engine == null) return;
        if(!(entity instanceof ItemEntity itemEntity)) return;
        if(!itemEntity.isAlive()) return;

        final var inserted = itemEntity.getItem().copy();
        final var remainder = engine.capability.insertItem(0, inserted, false);
        if(remainder.isEmpty()) {
            itemEntity.discard();
        } else if(remainder.getCount() < inserted.getCount()) {
            itemEntity.setItem(remainder);
        }
    }
}
