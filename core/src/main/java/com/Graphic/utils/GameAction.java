package com.Graphic.utils;

import com.badlogic.gdx.Input;

public enum GameAction {
    UP("Up", Input.Keys.UP),
    DOWN("Down", Input.Keys.DOWN),
    LEFT("Left", Input.Keys.LEFT),
    RIGHT("Right", Input.Keys.RIGHT),
    ATTACK("Attack", Input.Keys.Z),
    JUMP("Jump", Input.Keys.SPACE),
    DASH("Dash", Input.Keys.C),
    NAIL("Nail", Input.Keys.X),
    FOCUS_CAST("Focus / Cast", Input.Keys.A),
    Continue("Continue", Input.Keys.A),
    INVENTORY("Inventory", Input.Keys.I);

    private final String description;
    private final int defaultKeyCode;

    GameAction(String description, int defaultKeyCode) {
        this.description = description;
        this.defaultKeyCode = defaultKeyCode;
    }

    public String getDescription() {
        return description;
    }

    public int getDefaultKeyCode() {
        return defaultKeyCode;
    }
}
