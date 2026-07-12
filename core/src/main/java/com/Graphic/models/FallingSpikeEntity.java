package com.Graphic.models;

import com.Graphic.managers.AudioManager;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

public class FallingSpikeEntity {

    public enum State { IDLE, FALLING, LANDED }

    private static final float FALL_ACCELERATION = 1200f;
    private static final float MAX_FALL_SPEED    = 1000f;
    private static final float DAMAGE_IMPULSE_X  = 250f;
    private static final float DAMAGE_IMPULSE_Y  = 200f;

    private final Rectangle triggerZone;
    private final Rectangle bounds;

    private final Texture texture;

    private State state    = State.IDLE;
    private float velocity = 0f;

    public FallingSpikeEntity(FallingSpikeData data, Texture texture) {
        this.texture     = texture;
        this.triggerZone = data.triggerZone;


        this.bounds = new Rectangle(
            data.spawnX,
            data.spawnY,
            texture.getWidth(),
            texture.getHeight()
        );
    }






    public boolean update(float delta, Rectangle playerBounds,
                          Array<SolidBlock> solidBlocks) {
        switch (state) {
            case IDLE:
                if (playerBounds.overlaps(triggerZone)) {
                    state    = State.FALLING;
                    velocity = 0f;

                    AudioManager.playSFX("stalactite_break");
                }
                break;

            case FALLING:
                velocity = Math.min(velocity + FALL_ACCELERATION * delta, MAX_FALL_SPEED);
                bounds.y -= velocity * delta;


                for (SolidBlock b : solidBlocks) {
                    if (b.isDeadly) continue;
                    if (!bounds.overlaps(b.bounds)) continue;


                    bounds.y = b.bounds.y + b.bounds.height;
                    velocity = 0f;
                    state    = State.LANDED;


                    AudioManager.playSFX("stalactite_death");
                    AudioManager.playSFX("stalactite_impact");
                    return false;
                }

                if (bounds.overlaps(playerBounds)) {
                    return true;
                }
                break;

            case LANDED:
                break;
        }
        return false;
    }





    public void render(SpriteBatch batch) {
        if (state == State.IDLE) {

            batch.draw(texture, bounds.x, bounds.y, bounds.width, bounds.height);
        } else {
            batch.draw(texture, bounds.x, bounds.y, bounds.width, bounds.height);
        }
    }





    public boolean isDangerous() { return state == State.FALLING; }
    public boolean isLanded()    { return state == State.LANDED;  }
    public Rectangle getBounds() { return bounds; }
    public State     getState()  { return state;  }
    public Rectangle getTriggerZone() { return triggerZone; }

    public float getDamageImpulseX(Rectangle playerBounds) {

        float spikeCenter = bounds.x + bounds.width / 2f;
        float playerCenter = playerBounds.x + playerBounds.width / 2f;
        return playerCenter < spikeCenter ? -DAMAGE_IMPULSE_X : DAMAGE_IMPULSE_X;
    }
}

