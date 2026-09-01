package com.Graphic.models;

import com.Graphic.managers.AudioManager;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;

public class PressurePlate {

    public enum State { IDLE, SINKING, HELD_DOWN, RISING }

    private static final float PRESS_DEPTH = 200f;   // how far it sinks, per your spec
    private static final float SINK_SPEED = 900f;    // px/sec, snappy sink
    private static final float RISE_SPEED = 500f;    // px/sec, slightly slower rise
    private static final float HOLD_DURATION = 10f;  // stays down this long before popping back

    private final String groupId;
    private final Rectangle bounds;
    private final Texture texture;
    private final float width, height;

    private State state = State.IDLE;
    private float depth = 0f;       // current sink offset, 0 = up, PRESS_DEPTH = fully down
    private float holdTimer = 0f;

    public PressurePlate(String groupId, float x, float y, Texture texture) {
        this.groupId = groupId;
        this.texture = texture;
        this.width = texture.getWidth();
        this.height = texture.getHeight();
        this.bounds = new Rectangle(x - width / 2f, y - height / 2f, width, height);
    }

    /**
     * Returns true exactly on the frame the plate transitions IDLE -> SINKING,
     * i.e. the moment a puzzle challenge should be (re)rolled.
     */
    public boolean update(float delta, Rectangle playerBounds) {
        boolean triggeredThisFrame = false;

        switch (state) {
            case IDLE:
                if (bounds.overlaps(playerBounds)) {
                    state = State.SINKING;
                    AudioManager.playSFX("pressure_plate_press");
                    triggeredThisFrame = true;
                }
                break;

            case SINKING:
                depth = Math.min(PRESS_DEPTH, depth + SINK_SPEED * delta);
                if (depth >= PRESS_DEPTH) {
                    state = State.HELD_DOWN;
                    holdTimer = 0f;
                }
                break;

            case HELD_DOWN:
                holdTimer += delta;
                if (holdTimer >= HOLD_DURATION) {
                    state = State.RISING;
                }
                break;

            case RISING:
                depth = Math.max(0f, depth - RISE_SPEED * delta);
                if (depth <= 0f) {
                    state = State.IDLE;
                }
                break;
        }

        return triggeredThisFrame;
    }

    public void render(SpriteBatch batch) {
        batch.draw(texture, bounds.x, bounds.y - depth, width, height);
    }

    public State getState() { return state; }
    public float getHoldTimeRemaining() { return Math.max(0f, HOLD_DURATION - holdTimer); }
    public String getGroupId() { return groupId; }
    public Rectangle getBounds() { return bounds; }
}
