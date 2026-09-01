package com.Graphic.models.enums;

public enum PlatformDirection {
    UP(0f), RIGHT(270f), DOWN(180f), LEFT(90f);

    public final float angleDeg;
    PlatformDirection(float angleDeg) { this.angleDeg = angleDeg; }

    public PlatformDirection cw() {
        switch (this) {
            case UP:    return RIGHT;
            case RIGHT: return DOWN;
            case DOWN:  return LEFT;
            default:    return UP;
        }
    }

    public PlatformDirection ccw() {
        switch (this) {
            case UP:    return LEFT;
            case LEFT:  return DOWN;
            case DOWN:  return RIGHT;
            default:    return UP;
        }
    }

    public static PlatformDirection random() {
        PlatformDirection[] v = values();
        return v[com.badlogic.gdx.math.MathUtils.random(v.length - 1)];
    }
}
