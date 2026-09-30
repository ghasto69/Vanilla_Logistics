package com.ghasto.logistical_improvements;

import com.ghasto.logistical_improvements.combustion_engine.CombustionEngineBlockEntity;
import com.ghasto.logistical_improvements.combustion_engine.CombustionEngineRenderer;
import com.ghasto.logistical_improvements.combustion_engine.CombustionEngineVisual;
import com.tterrag.registrate.util.entry.BlockEntityEntry;

import static com.ghasto.logistical_improvements.VanillaLogistics.REGISTRATE;

public interface VLBlockEntities {
    BlockEntityEntry<CombustionEngineBlockEntity> COMBUSTION_ENGINE =
            REGISTRATE.blockEntity("combustion_engine", CombustionEngineBlockEntity::new)
                    .visual(() -> CombustionEngineVisual::new, false)
                    .validBlock(VLBlocks.COMBUSTION_ENGINE)
                    .renderer(() -> CombustionEngineRenderer::new)
                    .register();

    static void init() {}
}
