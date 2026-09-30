package com.ghasto.logistical_improvements.unloaded_conveyors;

public interface TrackedConveyor {
    /**
     * @return the game time at which this conveyor was last ticked by the server
     */
    long getLastServerTick();
}
