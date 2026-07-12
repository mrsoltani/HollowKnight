package com.Graphic.models;

import com.badlogic.gdx.math.Rectangle;

public class FallingSpikeData {
    public final Rectangle triggerZone;
    public final float     spawnX;
    public final float     spawnY;

    public FallingSpikeData(Rectangle triggerZone, float spawnX, float spawnY) {
        this.triggerZone = triggerZone;
        this.spawnX      = spawnX;
        this.spawnY      = spawnY;
    }
}
