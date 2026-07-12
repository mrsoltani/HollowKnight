package com.Graphic.models;

import com.badlogic.gdx.math.Rectangle;

public class TeleportZone {
    public final Rectangle bounds;
    public final String targetMapName;
    public final String targetSpawnName;

    public TeleportZone(Rectangle bounds, String targetMapName, String targetSpawnName) {
        this.bounds = bounds;
        this.targetMapName = targetMapName;
        this.targetSpawnName = (targetSpawnName == null || targetSpawnName.isEmpty()) ? "SpawnPlayer" : targetSpawnName;
    }
}
