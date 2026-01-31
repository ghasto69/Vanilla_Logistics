package com.ghasto.logistical_improvements;

import com.ghasto.logistical_improvements.combustion_engine.CombustionEngineBlock;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.foundation.data.BlockStateGen;
import com.simibubi.create.foundation.data.SharedProperties;
import com.simibubi.create.foundation.data.TagGen;
import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Items;

import static com.ghasto.logistical_improvements.VanillaLogistics.REGISTRATE;

public interface VLBlocks {
    BlockEntry<CombustionEngineBlock> COMBUSTION_ENGINE =
            REGISTRATE.block("combustion_engine", CombustionEngineBlock::new)
                    .initialProperties(SharedProperties::stone)
                    .properties(p -> p.noOcclusion())
                    .transform(TagGen.axeOrPickaxe())
                    .onRegister(BlockStressValues.setGeneratorSpeed(64, true))
                    .onRegister(block -> BlockStressValues.CAPACITIES.register(block, () -> 4096/64f)) //4096 su
                    .recipe((context, provider) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, context.get(), 1)
                            .define('F', Items.FURNACE)
                            .define('S', AllBlocks.SHAFT)
                            .define('C', AllBlocks.ANDESITE_CASING)
                            .define('A', AllItems.ANDESITE_ALLOY)
                            .pattern("SFS")
                            .pattern("ACA")
                            .unlockedBy("has_furnace", RegistrateRecipeProvider.has(Items.FURNACE)))
                    .blockstate(BlockStateGen.horizontalAxisBlockProvider(true))
                    .simpleItem()
                    .register();

    static void init() {}
}
