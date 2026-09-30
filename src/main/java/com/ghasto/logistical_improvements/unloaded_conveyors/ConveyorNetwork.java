package com.ghasto.logistical_improvements.unloaded_conveyors;

import com.ghasto.logistical_improvements.VanillaLogistics;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity.ConnectedPort;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorPackage;
import com.simibubi.create.content.logistics.box.PackageEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

/**
 * Keeps track of all connected chain conveyors of a level, in order to keep their packages moving while they are
 * not being ticked by the game.
 * <p>
 * Conveyors in chunks that are loaded but do not tick are simulated in place. Conveyors in unloaded chunks hand
 * their state over to a proxy, which is simulated instead and hands everything back once the chunk loads again.
 */
public class ConveyorNetwork extends SavedData {
    private static final String ID = VanillaLogistics.MODID + "_conveyor_network";
    private static final int VALIDATION_INTERVAL = 20;

    private final ServerLevel level;
    private final Map<BlockPos, ConveyorNode> nodes = new HashMap<>();
    private final Set<BlockPos> lookedUp = new HashSet<>();
    private ConveyorNode[] tickOrder;
    private boolean releasedSinceSave;
    private boolean closing;

    private ConveyorNetwork(ServerLevel level) {
        this.level = level;
    }

    public static ConveyorNetwork get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new Factory<>(() -> new ConveyorNetwork(level),
                (tag, registries) -> load(level, tag, registries)), ID);
    }

    public ServerLevel getLevel() {
        return level;
    }

    public Collection<ConveyorNode> getNodes() {
        return Collections.unmodifiableCollection(nodes.values());
    }

    @Nullable
    public ConveyorNode getNode(BlockPos pos) {
        return nodes.get(pos);
    }

    /**
     * @return the conveyor at the given position, or its stand-in if the chunk is not loaded
     */
    @Nullable
    public ChainConveyorBlockEntity getConveyor(BlockPos pos) {
        ConveyorNode node = nodes.get(pos);
        if (node != null && node.getConveyor() != null)
            return node.getConveyor();
        if (!level.isLoaded(pos) && !lookedUp.add(pos.immutable()))
            return null;
        // Conveyors that were not loaded ever since this mod got added are unknown until their chunk is loaded once
        return level.getBlockEntity(pos) instanceof ChainConveyorBlockEntity conveyor ? conveyor : null;
    }

    // Events

    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getChunk() instanceof LevelChunk chunk))
            return;
        for (BlockEntity blockEntity : chunk.getBlockEntities().values())
            if (blockEntity instanceof ChainConveyorBlockEntity conveyor)
                get(level).conveyorLoaded(conveyor, chunk);
    }

    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getChunk() instanceof LevelChunk chunk))
            return;
        for (BlockEntity blockEntity : chunk.getBlockEntities().values())
            if (blockEntity instanceof ChainConveyorBlockEntity conveyor)
                get(level).conveyorUnloaded(conveyor, chunk);
    }

    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level)
            get(level).tick();
    }

    public static void onLevelSave(LevelEvent.Save event) {
        // Chunks unloaded by the save took packages out of their conveyors after this data had already been written
        if (event.getLevel() instanceof ServerLevel level && get(level).releasedSinceSave)
            level.getDataStorage().save();
    }

    public static void onLevelUnload(LevelEvent.Unload event) {
        // Nothing gets saved past this point, packages have to stay with their conveyor
        if (event.getLevel() instanceof ServerLevel level)
            get(level).closing = true;
    }

    public void track(ChainConveyorBlockEntity conveyor) {
        ConveyorNode node = nodes.get(conveyor.getBlockPos());
        if (node == null) {
            if (conveyor.connections.isEmpty())
                return;
            node = addNode(conveyor.getBlockPos());
        } else if (node.loaded == conveyor) {
            if (conveyor.connections.isEmpty())
                removeNode(node);
            return;
        }

        if (claim(node, conveyor))
            conveyor.notifyUpdate();
    }

    public void untrack(ChainConveyorBlockEntity conveyor) {
        ConveyorNode node = nodes.get(conveyor.getBlockPos());
        if (node == null || node.loaded != null && node.loaded != conveyor)
            return;
        if (node.proxy != null)
            dropPackages(node.proxy);
        removeNode(node);
    }

    private void conveyorLoaded(ChainConveyorBlockEntity conveyor, LevelChunk chunk) {
        ConveyorNode node = nodes.get(conveyor.getBlockPos());
        if (node == null) {
            if (conveyor.connections.isEmpty())
                return;
            node = addNode(conveyor.getBlockPos());
        }

        if (node.loaded != conveyor && claim(node, conveyor))
            chunk.setUnsaved(true);
    }

    private void conveyorUnloaded(ChainConveyorBlockEntity conveyor, LevelChunk chunk) {
        ConveyorNode node = nodes.get(conveyor.getBlockPos());
        if (conveyor.connections.isEmpty()) {
            if (node != null && node.loaded == conveyor)
                removeNode(node);
            return;
        }
        if (node == null)
            node = addNode(conveyor.getBlockPos());

        release(node, conveyor, !closing);
        // The chunk must not be left with a copy of the packages that are moving on without it
        if (!closing)
            chunk.setUnsaved(true);
    }

    // Handing over state between conveyors and their stand-ins

    private boolean claim(ConveyorNode node, ChainConveyorBlockEntity conveyor) {
        ChainConveyorBlockEntity proxy = node.proxy;
        node.loaded = conveyor;
        node.proxy = null;
        setDirty();
        if (proxy == null)
            return false;

        conveyor.routingTable = proxy.routingTable;
        proxy.loopPorts.forEach(conveyor.loopPorts::putIfAbsent);
        proxy.travelPorts.forEach(conveyor.travelPorts::putIfAbsent);
        return movePackages(proxy, conveyor);
    }

    private void release(ConveyorNode node, ChainConveyorBlockEntity conveyor, boolean withPackages) {
        ChainConveyorBlockEntity proxy = node.proxy;
        if (proxy == null) {
            proxy = createProxy(node.pos);
            proxy.routingTable = conveyor.routingTable;
        }

        proxy.connections.clear();
        proxy.connections.addAll(conveyor.connections);
        proxy.connectionStats = null;
        proxy.loopPorts.clear();
        proxy.loopPorts.putAll(conveyor.loopPorts);
        proxy.travelPorts.clear();
        proxy.travelPorts.putAll(conveyor.travelPorts);
        proxy.setSpeed(conveyor.isOverStressed() ? 0 : conveyor.getTheoreticalSpeed());
        proxy.reversed = conveyor.reversed;
        if (withPackages)
            movePackages(conveyor, proxy);

        node.loaded = null;
        node.proxy = proxy;
        releasedSinceSave = true;
        setDirty();
    }

    private static boolean movePackages(ChainConveyorBlockEntity from, ChainConveyorBlockEntity to) {
        boolean moved = !from.getLoopingPackages().isEmpty();
        to.getLoopingPackages().addAll(from.getLoopingPackages());
        from.getLoopingPackages().clear();

        for (Entry<BlockPos, List<ChainConveyorPackage>> entry : from.getTravellingPackages().entrySet()) {
            if (entry.getValue().isEmpty())
                continue;
            moved = true;
            if (to.connections.contains(entry.getKey()))
                to.getTravellingPackages().computeIfAbsent(entry.getKey(), $ -> new ArrayList<>()).addAll(entry.getValue());
            else
                to.getLoopingPackages().addAll(entry.getValue());
        }
        from.getTravellingPackages().clear();
        return moved;
    }

    private ChainConveyorBlockEntity createProxy(BlockPos pos) {
        ChainConveyorBlockEntity proxy = new ChainConveyorBlockEntity(AllBlockEntityTypes.CHAIN_CONVEYOR.get(), pos,
                AllBlocks.CHAIN_CONVEYOR.getDefaultState());
        proxy.setLevel(level);
        proxy.checkInvalid = false;
        return proxy;
    }

    private void dropPackages(ChainConveyorBlockEntity proxy) {
        Vec3 dropPos = Vec3.atCenterOf(proxy.getBlockPos());
        List<ChainConveyorPackage> packages = new ArrayList<>(proxy.getLoopingPackages());
        proxy.getTravellingPackages().values().forEach(packages::addAll);
        for (ChainConveyorPackage box : packages)
            level.addFreshEntity(PackageEntity.fromItemStack(level, dropPos, box.item));
    }

    private ConveyorNode addNode(BlockPos pos) {
        ConveyorNode node = new ConveyorNode(pos);
        node.speedStamp = level.getGameTime();
        nodes.put(node.pos, node);
        tickOrder = null;
        setDirty();
        return node;
    }

    private void removeNode(ConveyorNode node) {
        nodes.remove(node.pos, node);
        tickOrder = null;
        setDirty();
    }

    // Ticking

    private void tick() {
        if (nodes.isEmpty() || !level.tickRateManager().runsNormally())
            return;
        if (tickOrder == null)
            tickOrder = nodes.values().toArray(ConveyorNode[]::new);

        long gameTime = level.getGameTime();
        for (ConveyorNode node : tickOrder) {
            if (nodes.get(node.pos) != node)
                continue;

            if (node.loaded != null && node.loaded.isRemoved()) {
                if (!node.loaded.isChunkUnloaded()) {
                    removeNode(node);
                    continue;
                }
                // Missed the unloading of the chunk, the packages of this conveyor got saved with it
                release(node, node.loaded, false);
            }

            if (node.loaded != null) {
                node.speedStamp = gameTime;
                if (((TrackedConveyor) node.loaded).getLastServerTick() == gameTime)
                    continue;
                ConveyorSimulation.tick(this, node.loaded);
                syncPackages(node, node.loaded);
                continue;
            }

            if (node.proxy == null) {
                removeNode(node);
                continue;
            }
            if (Math.floorMod(gameTime + node.pos.hashCode(), VALIDATION_INTERVAL) == 0 && !validate(node))
                continue;

            matchSpeedOfNeighbours(node, node.proxy);
            ConveyorSimulation.tick(this, node.proxy);
        }
    }

    /**
     * Stand-ins are only of use for as long as their chunk is unloaded and still contains their conveyor
     */
    private boolean validate(ConveyorNode node) {
        LevelChunk chunk = level.getChunkSource().getChunkNow(SectionPos.blockToSectionCoord(node.pos.getX()),
                SectionPos.blockToSectionCoord(node.pos.getZ()));
        if (chunk == null)
            return true;

        if (chunk.getBlockEntity(node.pos) instanceof ChainConveyorBlockEntity conveyor) {
            conveyorLoaded(conveyor, chunk);
            return false;
        }

        dropPackages(node.proxy);
        removeNode(node);
        return false;
    }

    /**
     * All conveyors of a chain share the same speed, so changes made to the loaded ones can be passed on to the others
     */
    private void matchSpeedOfNeighbours(ConveyorNode node, ChainConveyorBlockEntity proxy) {
        ConveyorNode mostRecent = null;
        for (BlockPos connection : proxy.connections) {
            ConveyorNode other = nodes.get(node.pos.offset(connection));
            if (other == null || other.getConveyor() == null)
                continue;
            if (mostRecent == null || other.speedStamp > mostRecent.speedStamp)
                mostRecent = other;
        }

        if (mostRecent == null || mostRecent.speedStamp - 1 <= node.speedStamp)
            return;

        node.speedStamp = mostRecent.speedStamp - 1;
        float speed = mostRecent.getConveyor().getSpeed();
        if (speed == proxy.getTheoreticalSpeed())
            return;
        proxy.setSpeed(speed);
        setDirty();
    }

    /**
     * Block entities of chunks that do not tick are not synced by the game
     */
    private void syncPackages(ConveyorNode node, ChainConveyorBlockEntity conveyor) {
        int packages = 1;
        for (ChainConveyorPackage box : conveyor.getLoopingPackages())
            packages = 31 * packages + box.netId;
        for (Entry<BlockPos, List<ChainConveyorPackage>> entry : conveyor.getTravellingPackages().entrySet())
            for (ChainConveyorPackage box : entry.getValue())
                packages = 31 * packages + entry.getKey().hashCode() + box.netId;

        if (packages == node.syncedPackages)
            return;
        node.syncedPackages = packages;

        List<ServerPlayer> players = level.getChunkSource().chunkMap.getPlayers(new ChunkPos(node.pos), false);
        if (players.isEmpty())
            return;
        ClientboundBlockEntityDataPacket packet = conveyor.getUpdatePacket();
        players.forEach(player -> player.connection.send(packet));
    }

    // Saving

    @Override
    public boolean isDirty() {
        // Packages move in and out of the stand-ins all the time
        return super.isDirty() || !nodes.isEmpty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        releasedSinceSave = false;
        ListTag conveyors = new ListTag();
        for (ConveyorNode node : nodes.values()) {
            ChainConveyorBlockEntity conveyor = node.getConveyor();
            if (conveyor == null)
                continue;

            CompoundTag nodeTag = new CompoundTag();
            nodeTag.put("Pos", NbtUtils.writeBlockPos(node.pos));
            nodeTag.putFloat("Speed", conveyor.isOverStressed() ? 0 : conveyor.getTheoreticalSpeed());
            nodeTag.putLong("SpeedStamp", node.speedStamp);

            ListTag connections = new ListTag();
            for (BlockPos connection : conveyor.connections)
                connections.add(NbtUtils.writeBlockPos(connection));
            nodeTag.put("Connections", connections);
            nodeTag.put("LoopPorts", writePorts(conveyor.loopPorts));
            nodeTag.put("TravelPorts", writePorts(conveyor.travelPorts));

            // Loaded conveyors save their own packages
            if (!node.isLoaded()) {
                nodeTag.put("LoopingPackages", writePackages(conveyor.getLoopingPackages(), registries));
                ListTag travelling = new ListTag();
                for (Entry<BlockPos, List<ChainConveyorPackage>> entry : conveyor.getTravellingPackages().entrySet()) {
                    CompoundTag travellingTag = new CompoundTag();
                    travellingTag.put("Target", NbtUtils.writeBlockPos(entry.getKey()));
                    travellingTag.put("Packages", writePackages(entry.getValue(), registries));
                    travelling.add(travellingTag);
                }
                nodeTag.put("TravellingPackages", travelling);
            }

            conveyors.add(nodeTag);
        }
        tag.put("Conveyors", conveyors);
        return tag;
    }

    public static ConveyorNetwork load(ServerLevel level, CompoundTag tag, HolderLookup.Provider registries) {
        ConveyorNetwork network = new ConveyorNetwork(level);
        for (Tag entry : tag.getList("Conveyors", Tag.TAG_COMPOUND)) {
            CompoundTag nodeTag = (CompoundTag) entry;
            BlockPos pos = NbtUtils.readBlockPos(nodeTag, "Pos").orElse(null);
            if (pos == null)
                continue;

            ChainConveyorBlockEntity proxy = network.createProxy(pos);
            proxy.setSpeed(nodeTag.getFloat("Speed"));
            for (Tag connection : nodeTag.getList("Connections", Tag.TAG_INT_ARRAY))
                readBlockPos(connection).ifPresent(proxy.connections::add);
            readPorts(nodeTag.getList("LoopPorts", Tag.TAG_COMPOUND), proxy.loopPorts);
            readPorts(nodeTag.getList("TravelPorts", Tag.TAG_COMPOUND), proxy.travelPorts);

            readPackages(nodeTag.getList("LoopingPackages", Tag.TAG_COMPOUND), proxy.getLoopingPackages(), registries);
            for (Tag travelling : nodeTag.getList("TravellingPackages", Tag.TAG_COMPOUND)) {
                CompoundTag travellingTag = (CompoundTag) travelling;
                BlockPos target = NbtUtils.readBlockPos(travellingTag, "Target").orElse(null);
                List<ChainConveyorPackage> packages = new ArrayList<>();
                readPackages(travellingTag.getList("Packages", Tag.TAG_COMPOUND), packages, registries);
                if (target != null && proxy.connections.contains(target))
                    proxy.getTravellingPackages().put(target, packages);
                else
                    proxy.getLoopingPackages().addAll(packages);
            }

            ConveyorNode node = new ConveyorNode(pos);
            node.speedStamp = nodeTag.getLong("SpeedStamp");
            node.proxy = proxy;
            network.nodes.put(node.pos, node);
        }
        return network;
    }

    private static ListTag writePorts(Map<BlockPos, ConnectedPort> ports) {
        ListTag list = new ListTag();
        ports.forEach((pos, port) -> {
            CompoundTag portTag = new CompoundTag();
            portTag.put("Pos", NbtUtils.writeBlockPos(pos));
            portTag.putFloat("ChainPosition", port.chainPosition());
            if (port.connection() != null)
                portTag.put("Connection", NbtUtils.writeBlockPos(port.connection()));
            portTag.putString("Filter", port.filter());
            list.add(portTag);
        });
        return list;
    }

    private static void readPorts(ListTag list, Map<BlockPos, ConnectedPort> ports) {
        for (Tag entry : list) {
            CompoundTag portTag = (CompoundTag) entry;
            NbtUtils.readBlockPos(portTag, "Pos").ifPresent(pos -> ports.put(pos,
                    new ConnectedPort(portTag.getFloat("ChainPosition"),
                            NbtUtils.readBlockPos(portTag, "Connection").orElse(null), portTag.getString("Filter"))));
        }
    }

    private static ListTag writePackages(List<ChainConveyorPackage> packages, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (ChainConveyorPackage box : packages)
            list.add(box.write(registries));
        return list;
    }

    private static void readPackages(ListTag list, List<ChainConveyorPackage> packages, HolderLookup.Provider registries) {
        for (Tag entry : list) {
            ChainConveyorPackage box = ChainConveyorPackage.read((CompoundTag) entry, registries);
            if (!box.item.isEmpty())
                packages.add(box);
        }
    }

    private static Optional<BlockPos> readBlockPos(Tag tag) {
        if (!(tag instanceof IntArrayTag array) || array.size() != 3)
            return Optional.empty();
        int[] coordinates = array.getAsIntArray();
        return Optional.of(new BlockPos(coordinates[0], coordinates[1], coordinates[2]));
    }
}
