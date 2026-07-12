package com.Graphic.utils;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

public class CharmSpawnData {
    public Vector2 animPos;
    public Rectangle triggerBox;

    public CharmSpawnData(Vector2 animPos, Rectangle triggerBox) {
        this.animPos = animPos;
        this.triggerBox = triggerBox;
    }
}
