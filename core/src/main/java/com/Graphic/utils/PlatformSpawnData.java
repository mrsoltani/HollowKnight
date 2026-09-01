package com.Graphic.utils;

import com.badlogic.gdx.math.Vector2;

/**
 * Spawn descriptor for a rotatable puzzle platform.
 * Read from the "platform_<groupId>_<n>" objects on the puzzle logic layer.
 * Coordinate system: Tiled y-down screen pixels used directly, same as every
 * other spawn point in this project (no flipping).
 */
public class PlatformSpawnData {
    public final String groupId;
    public final float x;
    public final float y;

    public PlatformSpawnData(String groupId, float x, float y) {
        this.groupId = groupId;
        this.x = x;
        this.y = y;
    }

    public Vector2 position() { return new Vector2(x, y); }
}
