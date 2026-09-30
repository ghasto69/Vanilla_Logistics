package com.ghasto.logistical_improvements.gametest;

import com.ghasto.logistical_improvements.VanillaLogistics;
import com.ghasto.logistical_improvements.unloaded_conveyors.ConveyorNetwork;
import com.ghasto.logistical_improvements.unloaded_conveyors.ConveyorNode;
import com.ghasto.logistical_improvements.unloaded_conveyors.TrackedConveyor;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorPackage;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.packagePort.PackagePortTarget.ChainConveyorFrogportTarget;
import com.simibubi.create.content.logistics.packagePort.frogport.FrogportBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@GameTestHolder(VanillaLogistics.MODID)
@PrefixGameTestTemplate(false)
public class UnloadedConveyorTests {
    // Far enough for the chunks to not be kept loaded by the test itself
    private static final int DISTANCE = 160;
    // Chunks this far away from any loaded chunk are not even kept in memory
    private static final int UNLOADING_DISTANCE = 320;

    @GameTest(template = "empty", timeoutTicks = 2400)
    public static void packagesTravelThroughUnloadedChunks(GameTestHelper helper) {
        sendPackageThroughUnloadedChunks(helper, UNLOADING_DISTANCE, true);
    }

    @GameTest(template = "empty", timeoutTicks = 1200)
    public static void packagesTravelThroughChunksKeptInMemory(GameTestHelper helper) {
        sendPackageThroughUnloadedChunks(helper, DISTANCE, false);
    }

    private static void sendPackageThroughUnloadedChunks(GameTestHelper helper, int distance, boolean finishUnloading) {
        ServerLevel level = helper.getLevel();
        ConveyorNetwork network = ConveyorNetwork.get(level);

        BlockPos start = helper.absolutePos(new BlockPos(1, 2, 1));
        BlockPos end = helper.absolutePos(new BlockPos(1, 2, 5));
        BlockPos farStart = start.offset(distance, 0, 0);
        BlockPos farEnd = end.offset(distance, 0, 0);
        BlockPos port = end.offset(2, 0, 0);
        List<BlockPos> conveyors = List.of(start, farStart, farEnd, end);

        forceChunks(level, true, farStart, farEnd);
        level.setBlockAndUpdate(start.below(), AllBlocks.CREATIVE_MOTOR.getDefaultState()
                .setValue(CreativeMotorBlock.FACING, Direction.UP));
        conveyors.forEach(pos -> level.setBlockAndUpdate(pos, AllBlocks.CHAIN_CONVEYOR.getDefaultState()));

        level.setBlockAndUpdate(port, AllBlocks.PACKAGE_FROGPORT.getDefaultState());
        FrogportBlockEntity frogport = (FrogportBlockEntity) level.getBlockEntity(port);
        frogport.addressFilter = "destination";
        frogport.target = new ChainConveyorFrogportTarget(end.subtract(port), 90, (BlockPos) null, false);

        ItemStack box = PackageItem.containing(List.of(new ItemStack(Items.DIAMOND)));
        PackageItem.addAddress(box, "destination");

        AtomicBoolean unloaded = new AtomicBoolean();
        AtomicBoolean passedUnloadedConveyors = new AtomicBoolean();
        helper.onEachTick(() -> {
            if (!unloaded.get())
                return;
            if (level.isLoaded(farStart) || level.isLoaded(farEnd))
                helper.fail("The chunks of the conveyors got loaded again");
            if (countPackages(network, farStart) + countPackages(network, farEnd) > 0)
                passedUnloadedConveyors.set(true);
        });

        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    connect(level, start, farStart);
                    connect(level, farStart, farEnd);
                    connect(level, farEnd, end);
                    ((CreativeMotorBlockEntity) level.getBlockEntity(start.below())).generatedSpeed.setValue(256);
                })
                .thenWaitUntil(() -> conveyors.forEach(pos -> {
                    assertTicking(helper, level, pos);
                    helper.assertTrue(((ChainConveyorBlockEntity) level.getBlockEntity(pos)).getSpeed() != 0, "Conveyor is not powered");
                }))
                .thenExecute(() -> forceChunks(level, false, farStart, farEnd))
                .thenWaitUntil(() -> {
                    assertUnloaded(helper, network, farStart, finishUnloading);
                    assertUnloaded(helper, network, farEnd, finishUnloading);
                })
                .thenExecute(() -> {
                    unloaded.set(true);
                    ((ChainConveyorBlockEntity) level.getBlockEntity(start)).addLoopingPackage(new ChainConveyorPackage(0, box));
                })
                .thenWaitUntil(() -> helper.assertTrue(ItemStack.isSameItemSameComponents(frogport.inventory.getStackInSlot(0), box),
                        "Package has not arrived"))
                .thenExecute(() -> {
                    helper.assertTrue(passedUnloadedConveyors.get(), "Package did not pass the unloaded conveyors");
                    helper.assertTrue(network.getNode(farStart).isLoaded() != finishUnloading, "Chunks did not stay as they were");
                })
                .thenSucceed();
    }

    @GameTest(template = "empty", timeoutTicks = 600)
    public static void packagesAreKeptWhenChunksUnloadAndLoad(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ConveyorNetwork network = ConveyorNetwork.get(level);

        BlockPos first = helper.absolutePos(new BlockPos(1, 2, 1)).offset(0, 0, UNLOADING_DISTANCE);
        BlockPos second = first.offset(4, 0, 0);

        forceChunks(level, true, first, second);
        level.setBlockAndUpdate(first, AllBlocks.CHAIN_CONVEYOR.getDefaultState());
        level.setBlockAndUpdate(second, AllBlocks.CHAIN_CONVEYOR.getDefaultState());

        ItemStack box = PackageItem.containing(List.of(new ItemStack(Items.DIAMOND)));
        PackageItem.addAddress(box, "nowhere");

        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    connect(level, first, second);
                    ((ChainConveyorBlockEntity) level.getBlockEntity(first)).addLoopingPackage(new ChainConveyorPackage(0, box));
                })
                .thenWaitUntil(() -> {
                    assertTicking(helper, level, first);
                    assertTicking(helper, level, second);
                })
                .thenExecute(() -> forceChunks(level, false, first, second))
                .thenWaitUntil(() -> {
                    assertUnloaded(helper, network, first, true);
                    assertUnloaded(helper, network, second, true);
                })
                .thenExecute(() -> {
                    helper.assertTrue(countPackages(network, first) == 1, "Package was not handed over by the unloading conveyor");

                    CompoundTag saved = network.save(new CompoundTag(), level.registryAccess());
                    ConveyorNetwork restored = ConveyorNetwork.load(level, saved, level.registryAccess());
                    helper.assertTrue(restored.getNodes().size() == network.getNodes().size(), "Conveyors were not saved");
                    helper.assertTrue(countPackages(restored, first) == 1, "Package was not saved");
                    helper.assertTrue(restored.getNode(first).getConveyor().connections.contains(second.subtract(first)),
                            "Connection was not saved");

                    forceChunks(level, true, first, second);
                })
                .thenWaitUntil(() -> {
                    ConveyorNode node = network.getNode(first);
                    helper.assertTrue(node != null && node.isLoaded() && level.isLoaded(first), "Conveyor is not loaded yet");
                    helper.assertTrue(level.getBlockEntity(first) == node.getConveyor(), "Loaded conveyor is not tracked");
                })
                .thenExecute(() -> {
                    ChainConveyorBlockEntity conveyor = (ChainConveyorBlockEntity) level.getBlockEntity(first);
                    List<ChainConveyorPackage> packages = conveyor.getLoopingPackages();
                    helper.assertTrue(packages.size() == 1 && ItemStack.isSameItemSameComponents(packages.get(0).item, box),
                            "Package was not handed back to the loaded conveyor");
                    forceChunks(level, false, first, second);
                })
                .thenSucceed();
    }

    private static void connect(ServerLevel level, BlockPos first, BlockPos second) {
        connectTo(level, first, second);
        connectTo(level, second, first);
    }

    private static void connectTo(ServerLevel level, BlockPos pos, BlockPos target) {
        ChainConveyorBlockEntity conveyor = (ChainConveyorBlockEntity) level.getBlockEntity(pos);
        conveyor.prepareStats();
        conveyor.addConnectionTo(target);
    }

    private static void forceChunks(ServerLevel level, boolean forced, BlockPos... positions) {
        for (BlockPos pos : positions)
            level.setChunkForced(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()), forced);
    }

    private static void assertTicking(GameTestHelper helper, ServerLevel level, BlockPos pos) {
        ConveyorNode node = ConveyorNetwork.get(level).getNode(pos);
        helper.assertTrue(node != null && node.isLoaded() && node.getConveyor() == level.getBlockEntity(pos),
                "Conveyor is not tracked");
        helper.assertTrue(((TrackedConveyor) node.getConveyor()).getLastServerTick() >= level.getGameTime() - 1,
                "Conveyor is not ticking");
    }

    private static void assertUnloaded(GameTestHelper helper, ConveyorNetwork network, BlockPos pos, boolean finishUnloading) {
        // The test server never has the spare time that chunks wait for before they unload
        if (finishUnloading)
            network.getLevel().getChunkSource().tick(() -> true, false);

        ConveyorNode node = network.getNode(pos);
        helper.assertFalse(network.getLevel().isLoaded(pos), "Chunk is still loaded");
        helper.assertTrue(node != null && node.getConveyor() != null, "Conveyor is not tracked");
        helper.assertTrue(node.isLoaded() != finishUnloading, "Chunk has not finished unloading");
    }

    private static int countPackages(ConveyorNetwork network, BlockPos pos) {
        ConveyorNode node = network.getNode(pos);
        if (node == null || node.getConveyor() == null || network.getLevel().isLoaded(pos))
            return 0;
        ChainConveyorBlockEntity conveyor = node.getConveyor();
        return conveyor.getLoopingPackages().size()
                + conveyor.getTravellingPackages().values().stream().mapToInt(List::size).sum();
    }
}
