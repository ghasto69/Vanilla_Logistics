package com.ghasto.logistical_improvements.gametest;

import com.ghasto.logistical_improvements.VLBlocks;
import com.ghasto.logistical_improvements.VanillaLogistics;
import com.ghasto.logistical_improvements.combustion_engine.CombustionEngineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

@GameTestHolder(VanillaLogistics.MODID)
@PrefixGameTestTemplate(false)
public class CombustionEngineTests {
    private static final BlockPos ENGINE = new BlockPos(3, 2, 3);

    @GameTest(template = "empty")
    public static void engineOnlyAcceptsFuel(GameTestHelper helper) {
        IItemHandler handler = placeEngine(helper);

        for (ItemStack stack : new ItemStack[]{new ItemStack(Items.COBBLESTONE, 16), new ItemStack(Items.LAVA_BUCKET)}) {
            ItemStack remainder = ItemHandlerHelper.insertItemStacked(handler, stack.copy(), false);
            if (!ItemStack.matches(remainder, stack))
                helper.fail("Inserting " + stack + " left " + remainder);
        }
        if (!engine(helper).inv.getStackInSlot(0).isEmpty())
            helper.fail("The engine holds " + engine(helper).inv.getStackInSlot(0));

        if (!ItemHandlerHelper.insertItemStacked(handler, new ItemStack(Items.COAL, 10), false).isEmpty())
            helper.fail("The engine did not accept coal");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void engineBurnsFuel(GameTestHelper helper) {
        IItemHandler handler = placeEngine(helper);
        ItemHandlerHelper.insertItemStacked(handler, new ItemStack(Items.COAL, 10), false);

        helper.runAfterDelay(10, () -> {
            CombustionEngineBlockEntity engine = engine(helper);
            if (!engine.fueled() || engine.getSpeed() != 64)
                helper.fail("The engine is not running, speed: " + engine.getSpeed());
            if (engine.inv.getStackInSlot(0).getCount() != 9)
                helper.fail("Expected 9 coal to be left, found " + engine.inv.getStackInSlot(0));
            if (!handler.extractItem(0, 64, false).isEmpty())
                helper.fail("Fuel could be taken back out of the engine");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void engineStopsWithoutFuel(GameTestHelper helper) {
        IItemHandler handler = placeEngine(helper);
        // Burns for 50 ticks
        ItemHandlerHelper.insertItemStacked(handler, new ItemStack(Items.BAMBOO), false);

        helper.runAfterDelay(10, () -> {
            if (!engine(helper).fueled())
                helper.fail("The engine did not start");
        });
        helper.runAfterDelay(80, () -> {
            CombustionEngineBlockEntity engine = engine(helper);
            if (engine.fueled() || engine.getSpeed() != 0)
                helper.fail("The engine is still running, speed: " + engine.getSpeed());
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void engineDropsFuelWhenBroken(GameTestHelper helper) {
        IItemHandler handler = placeEngine(helper);
        ItemHandlerHelper.insertItemStacked(handler, new ItemStack(Items.COAL, 10), false);

        helper.runAfterDelay(10, () -> {
            helper.getLevel().destroyBlock(helper.absolutePos(ENGINE), true);
            helper.assertItemEntityCountIs(Items.COAL, ENGINE, 2, 9);
            helper.assertItemEntityCountIs(VLBlocks.COMBUSTION_ENGINE.asItem(), ENGINE, 2, 1);
            helper.killAllEntitiesOfClass(ItemEntity.class);
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void engineCollectsFallenFuel(GameTestHelper helper) {
        placeEngine(helper);
        ItemEntity fuel = helper.spawnItem(Items.COAL, 3.5f, 3.5f, 3.5f);
        ItemEntity cobblestone = helper.spawnItem(Items.COBBLESTONE, 3.5f, 3.5f, 3.5f);

        helper.runAfterDelay(20, () -> {
            if (fuel.isAlive())
                helper.fail("The coal was not collected");
            if (!cobblestone.isAlive() || cobblestone.getItem().getCount() != 1)
                helper.fail("The cobblestone was collected");
            if (!engine(helper).fueled())
                helper.fail("The engine is not running");
            cobblestone.discard();
            helper.succeed();
        });
    }

    private static IItemHandler placeEngine(GameTestHelper helper) {
        helper.setBlock(ENGINE, VLBlocks.COMBUSTION_ENGINE.getDefaultState());
        IItemHandler handler = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(ENGINE), null);
        if (handler == null)
            helper.fail("The engine has no item handler");
        return handler;
    }

    private static CombustionEngineBlockEntity engine(GameTestHelper helper) {
        return helper.getBlockEntity(ENGINE);
    }
}
