package com.ghasto.logistical_improvements.combustion_engine;

import com.ghasto.logistical_improvements.VLBlockEntities;
import com.simibubi.create.content.kinetics.base.HorizontalAxisKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.createmod.catnip.data.Iterate;
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
    public static final VoxelShape SHAPE = Shapes.join(Block.box(1, 0, 1, 15, 2, 15), Stream.of(
            Block.box(1, 0, 0, 15, 2, 1),
            Block.box(1, 2, 0, 15, 3, 1),
            Block.box(1, 0, 15, 15, 2, 16),
            Block.box(1, 2, 15, 15, 3, 16),
            Block.box(4, 2, 1, 12, 13, 15)
    ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get(), BooleanOp.OR);

    public static final VoxelShape SHAPE_ROTATED = Shapes.join(Block.box(1, 0, 1, 15, 2, 15), Stream.of(
            Block.box(15, 0, 1, 16, 2, 15),   // Previously 1, 0, 0 to 15, 2, 1
            Block.box(15, 2, 1, 16, 3, 15),   // Previously 1, 2, 0 to 15, 3, 1
            Block.box(0, 0, 1, 1, 2, 15),     // Previously 1, 0, 15 to 15, 2, 16
            Block.box(0, 2, 1, 1, 3, 15),     // Previously 1, 2, 15 to 15, 3, 16
            Block.box(1, 2, 4, 15, 13, 12)    // Previously 4, 2, 1 to 12, 13, 15
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
        return state.getValue(HORIZONTAL_AXIS) == Direction.Axis.Z ? SHAPE : SHAPE_ROTATED;
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

        final var inserted = itemEntity.getItem().copy();
        if (!CombustionEngineBlockEntity.canInsert(inserted)) return;

        final var remainder = engine.capability.insertItem(0, inserted, false);
        if(remainder.isEmpty()) {
            itemEntity.kill();
        }
        int diff = inserted.getCount() - remainder.getCount();
        itemEntity.getItem().shrink(diff);
    }
}
