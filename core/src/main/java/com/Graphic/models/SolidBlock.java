package com.Graphic.models;

import com.badlogic.gdx.math.Rectangle;

public class SolidBlock {
    public Rectangle bounds;
    public boolean isDeadly;
    public boolean slide = true;

    public SolidBlock(float x, float y, float width, float height, boolean isDeadly,boolean slide) {
        this.bounds = new Rectangle(x, y, width, height);
        this.isDeadly = isDeadly;
        this.slide = slide;
    }
}
