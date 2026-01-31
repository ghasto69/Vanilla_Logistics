package com.ghasto.logistical_improvements;

import com.ghasto.logistical_improvements.combustion_engine.CombustionEngineBlockEntity;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;
import com.tterrag.registrate.util.entry.BlockEntityEntry;

import static com.ghasto.logistical_improvements.VanillaLogistics.REGISTRATE;

public interface VLBlockEntities {
    BlockEntityEntry<CombustionEngineBlockEntity> COMBUSTION_ENGINE =
            REGISTRATE.blockEntity("combustion_engine", CombustionEngineBlockEntity::new)
                    .visual(() -> SingleAxisRotatingVisual.of(AllPartialModels.SHAFT), false)
                    .validBlock(VLBlocks.COMBUSTION_ENGINE)
                    .register();

    static void init() {}
}
