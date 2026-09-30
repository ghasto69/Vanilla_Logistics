package com.ghasto.logistical_improvements.unloaded_conveyors;

import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity.ConnectedPort;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity.ConnectionStats;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorPackage;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.packagePort.frogport.FrogportBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Moves the packages of a conveyor that is not being ticked by the game. Follows ChainConveyorBlockEntity#tick, but
 * never loads a chunk to do so.
 */
public class ConveyorSimulation {
    private static final float RADIUS = 1.5f;
    private static final float OFF_BRANCH_DISTANCE = 35f;
    private static final int PORT_REFRESH_INTERVAL = 10;

    public static void tick(ConveyorNetwork network, ChainConveyorBlockEntity conveyor) {
        ServerLevel level = network.getLevel();
        BlockPos pos = conveyor.getBlockPos();

        float speed = conveyor.getSpeed() / 360f;
        float distancePerTick = Math.abs(speed);
        float degreesPerTick = (speed / (Mth.PI * RADIUS)) * 360f;
        boolean reversedPreviously = conveyor.reversed;

        conveyor.prepareStats();
        tickRouting(network, conveyor);

        if (speed == 0) {
            conveyor.updateBoxWorldPositions();
            return;
        }

        boolean changed = false;

        if (reversedPreviously != conveyor.reversed) {
            for (Entry<BlockPos, List<ChainConveyorPackage>> entry : conveyor.getTravellingPackages().entrySet()) {
                BlockPos offset = entry.getKey();
                ChainConveyorBlockEntity other = network.getConveyor(pos.offset(offset));
                if (other == null)
                    continue;
                for (Iterator<ChainConveyorPackage> iterator = entry.getValue().iterator(); iterator.hasNext(); ) {
                    ChainConveyorPackage box = iterator.next();
                    if (box.justFlipped)
                        continue;
                    box.justFlipped = true;
                    float length = (float) Vec3.atLowerCornerOf(offset).length() - 22 / 16f;
                    box.chainPosition = length - box.chainPosition;
                    other.addTravellingPackage(box, offset.multiply(-1));
                    iterator.remove();
                    changed = true;
                }
            }
        }

        for (Entry<BlockPos, List<ChainConveyorPackage>> entry : conveyor.getTravellingPackages().entrySet()) {
            BlockPos target = entry.getKey();
            ConnectionStats stats = conveyor.connectionStats.get(target);
            if (stats == null)
                continue;

            Travelling:
            for (Iterator<ChainConveyorPackage> iterator = entry.getValue().iterator(); iterator.hasNext(); ) {
                ChainConveyorPackage box = iterator.next();
                box.justFlipped = false;

                float prevChainPosition = box.chainPosition;
                box.chainPosition = Math.min(stats.chainLength(), box.chainPosition + distancePerTick);

                for (Entry<BlockPos, ConnectedPort> portEntry : conveyor.travelPorts.entrySet()) {
                    ConnectedPort port = portEntry.getValue();
                    if (prevChainPosition > port.chainPosition() || box.chainPosition < port.chainPosition())
                        continue;
                    if (!target.equals(port.connection()))
                        continue;
                    if (!PackageItem.matchAddress(box.item, port.filter()))
                        continue;
                    if (!exportToPort(level, box, pos.offset(portEntry.getKey())))
                        continue;

                    iterator.remove();
                    changed = true;
                    continue Travelling;
                }

                if (box.chainPosition < stats.chainLength())
                    continue;

                ChainConveyorBlockEntity other = network.getConveyor(pos.offset(target));
                if (other == null)
                    continue;

                box.chainPosition = conveyor.wrapAngle(stats.tangentAngle() + 180
                        + 2 * OFF_BRANCH_DISTANCE * (conveyor.reversed ? -1 : 1));
                other.addLoopingPackage(box);
                iterator.remove();
                changed = true;
            }
        }

        Looping:
        for (Iterator<ChainConveyorPackage> iterator = conveyor.getLoopingPackages().iterator(); iterator.hasNext(); ) {
            ChainConveyorPackage box = iterator.next();
            box.justFlipped = false;

            float prevChainPosition = box.chainPosition;
            box.chainPosition = conveyor.wrapAngle(box.chainPosition + degreesPerTick);

            for (Entry<BlockPos, ConnectedPort> portEntry : conveyor.loopPorts.entrySet()) {
                ConnectedPort port = portEntry.getValue();
                if (!conveyor.loopThresholdCrossed(box.chainPosition, prevChainPosition, port.chainPosition()))
                    continue;
                if (!PackageItem.matchAddress(box.item, port.filter()))
                    continue;
                if (!exportToPort(level, box, pos.offset(portEntry.getKey())))
                    continue;

                iterator.remove();
                changed = true;
                continue Looping;
            }

            for (BlockPos connection : conveyor.connections) {
                ConnectionStats stats = conveyor.connectionStats.get(connection);
                if (stats == null)
                    continue;
                if (!conveyor.loopThresholdCrossed(box.chainPosition, prevChainPosition, stats.tangentAngle()))
                    continue;
                if (!conveyor.routingTable.getExitFor(box.item).equals(connection))
                    continue;

                ChainConveyorBlockEntity other = network.getConveyor(pos.offset(connection));
                if (other != null && !other.canAcceptMorePackagesFromOtherConveyor())
                    continue;

                box.chainPosition = 0;
                conveyor.addTravellingPackage(box, connection);
                iterator.remove();
                changed = true;
                continue Looping;
            }
        }

        if (changed)
            conveyor.notifyUpdate();
        conveyor.updateBoxWorldPositions();
    }

    private static void tickRouting(ConveyorNetwork network, ChainConveyorBlockEntity conveyor) {
        ServerLevel level = network.getLevel();
        BlockPos pos = conveyor.getBlockPos();

        // Frogports refresh their routing entry whenever they tick, so this has to fill in for those that currently don't
        if (Math.floorMod(level.getGameTime() + pos.hashCode(), PORT_REFRESH_INTERVAL) == 0) {
            refreshPorts(level, conveyor, conveyor.loopPorts);
            refreshPorts(level, conveyor, conveyor.travelPorts);
        }

        conveyor.routingTable.tick();
        if (!conveyor.routingTable.shouldAdvertise())
            return;

        for (BlockPos connection : conveyor.connections) {
            ChainConveyorBlockEntity other = network.getConveyor(pos.offset(connection));
            if (other != null)
                conveyor.routingTable.advertiseTo(connection, other.routingTable);
        }
        conveyor.routingTable.changed = false;
        conveyor.routingTable.lastUpdate = 0;
    }

    private static void refreshPorts(ServerLevel level, ChainConveyorBlockEntity conveyor, Map<BlockPos, ConnectedPort> ports) {
        for (Iterator<Entry<BlockPos, ConnectedPort>> iterator = ports.entrySet().iterator(); iterator.hasNext(); ) {
            Entry<BlockPos, ConnectedPort> entry = iterator.next();
            ConnectedPort port = entry.getValue();
            BlockPos portPos = conveyor.getBlockPos().offset(entry.getKey());

            // Ports in unloaded chunks cannot have changed since they were registered
            if (level.isLoaded(portPos) && !isPortStillValid(level, portPos, port)) {
                iterator.remove();
                continue;
            }

            BlockPos connection = port.connection();
            if (connection != null && !conveyor.connections.contains(connection))
                continue;
            conveyor.routingTable.receivePortInfo(port.filter(), connection == null ? BlockPos.ZERO : connection);
        }
    }

    private static boolean isPortStillValid(ServerLevel level, BlockPos portPos, ConnectedPort port) {
        return level.getBlockEntity(portPos) instanceof FrogportBlockEntity frogport
                && port.filter().equals(frogport.getFilterString());
    }

    private static boolean exportToPort(ServerLevel level, ChainConveyorPackage box, BlockPos portPos) {
        // A frogport that is not ticking would not be able to finish catching the package
        if (!level.isLoaded(portPos) || !level.shouldTickBlocksAt(portPos))
            return false;
        if (!(level.getBlockEntity(portPos) instanceof FrogportBlockEntity frogport))
            return false;
        if (frogport.isAnimationInProgress() || frogport.isBackedUp())
            return false;

        frogport.startAnimation(box.item, false);
        return true;
    }
}
