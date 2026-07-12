package com.Graphic.models;

import com.Graphic.managers.AudioManager;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;

public class BreakableWallEntity {

    private static final int   MAX_HITS        = 3;
    private static final float SHAKE_DURATION  = 0.25f;
    private static final float SHAKE_INTENSITY = 6f;

    private final Rectangle bounds;
    private int   hitCount   = 0;
    private boolean broken   = false;

    private float shakeTimer   = 0f;
    public  float shakeOffsetX = 0f;
    public  float shakeOffsetY = 0f;

    public BreakableWallEntity(Rectangle bounds) {
        this.bounds = bounds;
    }

    public void update(float delta) {
        if (shakeTimer > 0) {
            shakeTimer -= delta;
            float power = SHAKE_INTENSITY * (shakeTimer / SHAKE_DURATION);
            shakeOffsetX = (MathUtils.random() - 0.5f) * 2f * power;
            shakeOffsetY = (MathUtils.random() - 0.5f) * 2f * power;
        } else {
            shakeOffsetX = 0f;
            shakeOffsetY = 0f;
        }
    }


    public boolean onHit() {
        if (broken) return false;
        hitCount++;
        shakeTimer = SHAKE_DURATION;

        if (hitCount >= MAX_HITS) {
            broken = true;

            AudioManager.playSFX("wall_break_death");
            return true;
        }


        String randomHitSFX = MathUtils.randomBoolean() ? "wall_break_hit_1" : "wall_break_hit_2";
        AudioManager.playPitchedSFX(0.9f, 1.1f, randomHitSFX);

        return false;
    }

    public boolean isBroken()    { return broken; }
    public boolean isIntact()    { return !broken; }
    public Rectangle getBounds() { return bounds; }
}
