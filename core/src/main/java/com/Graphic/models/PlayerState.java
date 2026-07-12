package com.Graphic.models;

/**
 * Reconstructed from every value referenced in Player.java (both the
 * original and the Focus/pogo-updated version) since I don't have the
 * actual file — please diff this against your real PlayerState.java in
 * case it has additional states not yet used there.
 */
public enum PlayerState {
    IDLE,
    RUN,
    RUN_TO_IDLE,
    AIRBORNE,
    FALL,
    ATTACK,
    ATTACK_ALT,   // new — paired with the "Slash Alt" atlas region
    DOWN_SLASH,
    IDLE_HURT,
    DEAD,
    DASH,
    SHADOW_DASH,
    DOUBLE_JUMP,
    FOCUS_START,
    FOCUS,
    FOCUS_GET,
    FOCUS_END,
    WALL_JUMP,
    WALL_SLIDE,
    FIREBALL_CAST,
    SCREAM_CAST
}
