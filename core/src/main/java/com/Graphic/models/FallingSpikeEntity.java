package com.Graphic.models;

import com.Graphic.managers.AudioManager;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

public class FallingSpikeEntity {

    public enum State { IDLE, FALLING, LANDED }

    private static final float FALL_ACCELERATION = 1200f;  // pixels/sec²
    private static final float MAX_FALL_SPEED    = 1000f;
    private static final float DAMAGE_IMPULSE_X  = 250f;   // knockback on player hit
    private static final float DAMAGE_IMPULSE_Y  = 200f;

    private final Rectangle triggerZone; // player enters this → spike falls
    private final Rectangle bounds;      // spike hitbox (also used for landing)

    private final Texture texture;

    private State state    = State.IDLE;
    private float velocity = 0f;         // downward speed, positive = down

    public FallingSpikeEntity(FallingSpikeData data, Texture texture) {
        this.texture     = texture;
        this.triggerZone = data.triggerZone;

        // Spike bounds sized to the texture, positioned at spawn point
        this.bounds = new Rectangle(
            data.spawnX,
            data.spawnY,
            texture.getWidth(),
            texture.getHeight()
        );
    }

    // =========================================================================
    // Update
    // =========================================================================

    /**
     * @param playerBounds  live player hitbox — used for both trigger and damage
     * @param solidBlocks   map geometry — spike lands on non-deadly blocks
     * @return true if the spike hit the player this frame (GameScreen applies damage)
     */
    public boolean update(float delta, Rectangle playerBounds,
                          Array<SolidBlock> solidBlocks) {
        switch (state) {
            case IDLE:
                if (playerBounds.overlaps(triggerZone)) {
                    state    = State.FALLING;
                    velocity = 0f;
                    // Play the snap/crack sound when the stalactite breaks loose from the ceiling
                    AudioManager.playSFX("stalactite_break");
                }
                break;

            case FALLING:
                velocity = Math.min(velocity + FALL_ACCELERATION * delta, MAX_FALL_SPEED);
                bounds.y -= velocity * delta;

                // ── Check landing on solid blocks ──────────────────────────
                for (SolidBlock b : solidBlocks) {
                    if (b.isDeadly) continue;           // don't land on spikes
                    if (!bounds.overlaps(b.bounds)) continue;

                    // Settle the spike on top of the block
                    bounds.y = b.bounds.y + b.bounds.height;
                    velocity = 0f;
                    state    = State.LANDED;

                    // Play shatter death audio when it strikes the ground
                    AudioManager.playSFX("stalactite_death");
                    AudioManager.playSFX("stalactite_impact");
                    return false;
                }

                if (bounds.overlaps(playerBounds)) {
                    return true;          // tell GameScreen to apply damage
                }
                break;

            case LANDED:
                break;
        }
        return false;
    }

    // =========================================================================
    // Render — always draw (background level), no visibility gating
    // =========================================================================

    public void render(SpriteBatch batch) {
        if (state == State.IDLE) {
            // Draw at spawn position even while idle
            batch.draw(texture, bounds.x, bounds.y, bounds.width, bounds.height);
        } else {
            batch.draw(texture, bounds.x, bounds.y, bounds.width, bounds.height);
        }
    }

    // =========================================================================
    // Queries
    // =========================================================================

    public boolean isDangerous() { return state == State.FALLING; }
    public boolean isLanded()    { return state == State.LANDED;  }
    public Rectangle getBounds() { return bounds; }
    public State     getState()  { return state;  }
    public Rectangle getTriggerZone() { return triggerZone; }

    public float getDamageImpulseX(Rectangle playerBounds) {
        // Push player away from spike center
        float spikeCenter = bounds.x + bounds.width / 2f;
        float playerCenter = playerBounds.x + playerBounds.width / 2f;
        return playerCenter < spikeCenter ? -DAMAGE_IMPULSE_X : DAMAGE_IMPULSE_X;
    }
}

