package com.Graphic.models;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

/**
 * Spawn descriptor for a downward map laser hazard.
 * Created by TiledMapHelper when reading the "laserSpawn" object layer.
 *
 * Coordinate system: Tiled y-down screen pixels, used directly throughout the
 * game (player bounds, solid-block bounds, enemy spawns are all read straight
 * from Tiled without flipping). The beam therefore fires from the authored
 * point straight down toward higher Y values.
 */
public class LaserSpawnData {
    public final float x;
    public final float y;
    public final float beamWidth;
    public final float beamHeight;

    public LaserSpawnData(float x, float y, float beamWidth, float beamHeight) {
        this.x = x;
        this.y = y;
        this.beamWidth = beamWidth;
        this.beamHeight = beamHeight;
    }

    public Vector2 position() {
        return new Vector2(x, y);
    }

    public Rectangle spawnRect() {
        return new Rectangle(x - beamWidth / 2f, y, beamWidth, 0f);
    }
}