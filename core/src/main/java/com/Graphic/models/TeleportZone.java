package com.Graphic.models;

import com.badlogic.gdx.math.Rectangle;

public class TeleportZone {
    public final Rectangle bounds;
    public final String targetMapName;  // Stores e.g., "CROSSROADS_02"
    public final String targetSpawnName; // Stores e.g., "SPAWN_WEST"

    public TeleportZone(Rectangle bounds, String targetMapName, String targetSpawnName) {
        this.bounds = bounds;
        this.targetMapName = targetMapName;
        this.targetSpawnName = (targetSpawnName == null || targetSpawnName.isEmpty()) ? "SpawnPlayer" : targetSpawnName;
    }
}
