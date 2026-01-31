package com.ghasto.logistical_improvements;

import com.simibubi.create.api.registry.CreateRegistries;
import com.simibubi.create.content.kinetics.mechanicalArm.AllArmInteractionPointTypes;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPointType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import static com.ghasto.logistical_improvements.VanillaLogistics.REGISTRATE;

public interface VLArmInteractionTypes {
    static void init() {
        REGISTRATE.simple("combustion_engine", CreateRegistries.ARM_INTERACTION_POINT_TYPE, CombustionEngineType::new);
    }

    class CombustionEngineType extends ArmInteractionPointType {
        @Override
        public boolean canCreatePoint(Level level, BlockPos pos, BlockState state) {
            return VLBlocks.COMBUSTION_ENGINE.has(state);
        }

        @Override
        public @Nullable ArmInteractionPoint createPoint(Level level, BlockPos pos, BlockState state) {
            return new CombustionEnginePoint(this, level, pos, state);
        }
    }

    class CombustionEnginePoint extends AllArmInteractionPointTypes.DepositOnlyArmInteractionPoint {
        public CombustionEnginePoint(ArmInteractionPointType type, Level level, BlockPos pos, BlockState state) {
            super(type, level, pos, state);
        }
    }
}
