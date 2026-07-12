package com.Graphic.models.spells;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

/**
 * Fireball (SoulBall / ShadowBall) spell projectile.
 *
 * Behaviour:
 *  - Travels in a straight line in the cast direction.
 *  - Animation plays forward once, then HOLDS on the last frame while it keeps traveling.
 *  - When it hits something (call hitWall()), it switches into VANISHING and plays the
 *    same frames back in REVERSE until it reaches frame 0, then FINISHED.
 *  - Hitbox is a plain Rectangle anchored to the FRONT EDGE of the current sprite frame
 *    (not the sprite's center point) and extends forward by HITBOX_WIDTH.
 *  - The hitbox never expires on a timer ("eternal") while TRAVELING - it only goes away
 *    when the fireball itself vanishes. Each fireball only damages a given target once
 *    (tracked internally) so it doesn't melt one enemy over many overlapping frames.
 */
public class Fireball {

    public enum Phase { TRAVELING, VANISHING, FINISHED }

    // ---- Tunable hitbox ----
    public static float HITBOX_WIDTH  = 30f;
    public static float HITBOX_HEIGHT = 70f;

    // ---- Tunable motion / timing ----
    public static float SPEED          = 100f;   // px/sec
    public static float FRAME_DURATION = 1f / 14f;

    // ---- Damage ----
    private static final float BASE_DAMAGE            = 15f;
    private static final float VOID_HEART_DAMAGE_MULT = 1.5f;

    private final Array<TextureAtlas.AtlasRegion> frames;
    private int   frameIndex = 0;
    private float frameTimer = 0f;

    private Phase phase = Phase.TRAVELING;

    private final Vector2 position = new Vector2();
    private final boolean facingRight;
    private final boolean voidHeartActive;
    private final float   damage;

    private final Rectangle hitbox = new Rectangle();
    private final Array<Damageable> hitTargets = new Array<>();

    public Fireball(TextureAtlas atlas, float startX, float startY, boolean facingRight, boolean voidHeartActive) {
        this.facingRight     = facingRight;
        this.voidHeartActive = voidHeartActive;
        this.damage           = voidHeartActive ? BASE_DAMAGE * VOID_HEART_DAMAGE_MULT : BASE_DAMAGE;

        String regionName = voidHeartActive ? "ShadowBall" : "SoulBall";
        this.frames = atlas.findRegions(regionName);
        if (this.frames.size == 0) {
            throw new IllegalStateException("No regions found for '" + regionName + "' in Ball.atlas");
        }

        position.set(startX, startY);
        updateHitbox();
    }

    public void update(float delta) {
        if (phase == Phase.FINISHED) return;

        if (phase == Phase.TRAVELING) {
            position.x += (facingRight ? SPEED : -SPEED) * delta;

            // Advance forward through the frames, but stop once we hit the last one
            // -> that's the "hold on last frame" behaviour.
            if (frameIndex < frames.size - 1) {
                frameTimer += delta;
                while (frameTimer >= FRAME_DURATION && frameIndex < frames.size - 1) {
                    frameTimer -= FRAME_DURATION;
                    frameIndex++;
                }
            }
        } else { // VANISHING
            frameTimer += delta;
            while (frameTimer >= FRAME_DURATION && frameIndex > 0) {
                frameTimer -= FRAME_DURATION;
                frameIndex--;
            }
            if (frameIndex <= 0) {
                FRAME_DURATION*=4;
                phase = Phase.FINISHED;
            }
        }

        updateHitbox();
    }

    private void updateHitbox() {
        TextureRegion frame = getFrame();
        float imgHalfWidth = frame.getRegionWidth() / 2f;

        // Front edge of the currently drawn sprite (sprite is centered on `position`).
        float frontEdgeX = facingRight ? position.x + imgHalfWidth : position.x - imgHalfWidth;
        float x = facingRight ? frontEdgeX : frontEdgeX - HITBOX_WIDTH;

        hitbox.set(x, position.y - HITBOX_HEIGHT / 2f, HITBOX_WIDTH, HITBOX_HEIGHT);
    }

    /** Call this the moment the projectile overlaps a wall / solid block. */
    public void hitWall() {
        if (phase == Phase.TRAVELING) {
            FRAME_DURATION/=4;
            phase      = Phase.VANISHING;
            frameTimer = 0f;
        }
    }

    /** True once already damaged by this exact fireball instance. */
    public boolean hasHit(Damageable target) { return hitTargets.contains(target, true); }

    public void registerHit(Damageable target) { hitTargets.add(target); }

    public TextureRegion getFrame()      { return frames.get(frameIndex); }
    public Rectangle      getHitbox()     { return hitbox; }
    public Vector2         getPosition()   { return position; }
    public Phase           getPhase()      { return phase; }
    public boolean         isFinished()    { return phase == Phase.FINISHED; }
    public boolean         isFacingRight() { return facingRight; }
    public boolean         isVoidHeartActive() { return voidHeartActive; }
    public float           getDamage()     { return damage; }

    public void render(SpriteBatch batch) {
        TextureRegion frame = getFrame();
        float w = frame.getRegionWidth();
        float h = frame.getRegionHeight();
        float drawX = position.x - w / 2f;
        float drawY = position.y - h / 2f;

        if (facingRight) {
            batch.draw(frame, drawX, drawY, w, h);
        } else {
            batch.draw(frame, drawX + w, drawY, -w, h);
        }
    }
}
