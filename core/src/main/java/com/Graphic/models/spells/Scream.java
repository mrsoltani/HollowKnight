package com.Graphic.models.spells;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

/**
 * Scream (SoulScream / ShadowScream) spell - the "Howling Wraiths" style cone attack.
 *
 * Behaviour:
 *  - Position is fixed at cast time (doesn't move, unlike Fireball).
 *  - Animation plays forward once. As soon as the last frame's duration elapses it is
 *    marked FINISHED (vanishes at the last frame - no hold, no reverse).
 *  - Hitbox is a symmetric isoceles triangle: apex sits at the cast position, opening
 *    upward. HEIGHT is the apex->base distance, ANGLE_DEGREES is the full spread angle.
 *  - Unlike Fireball, the hitbox is only ACTIVE for HITBOX_LIFETIME_TICKS update() calls
 *    (default 3), regardless of how long the full animation keeps playing afterwards.
 *    Each scream instance also only damages a given target once.
 */
public class Scream {

    public enum Phase { ACTIVE, FINISHED }

    // ---- Tunable hitbox ----
    public static float HEIGHT        = 200f;  // apex -> base distance
    public static float ANGLE_DEGREES = 60f;   // full spread angle of the cone

    // ---- Tunable timing ----
    public static float FRAME_DURATION       = 1f / 15f;
    public static int   HITBOX_LIFETIME_TICKS = 3; // how many update() calls the hitbox stays live for

    // ---- Damage ----
    private static final float BASE_DAMAGE            = 20f;
    private static final float VOID_HEART_DAMAGE_MULT = 1.5f;

    private final Array<TextureAtlas.AtlasRegion> frames;
    private int   frameIndex = 0;
    private float frameTimer = 0f;
    private int   hitboxTicksElapsed = 0;

    private Phase phase = Phase.ACTIVE;

    private final Vector2 position = new Vector2();
    private final boolean voidHeartActive;
    private final float   damage;

    private final Polygon hitboxPolygon = new Polygon();
    private final Array<Damageable> hitTargets = new Array<>();

    public Scream(TextureAtlas atlas, float x, float y, boolean voidHeartActive) {
        this.voidHeartActive = voidHeartActive;
        this.damage           = voidHeartActive ? BASE_DAMAGE * VOID_HEART_DAMAGE_MULT : BASE_DAMAGE;

        String regionName = voidHeartActive ? "ShadowScream" : "SoulScream";
        this.frames = atlas.findRegions(regionName);
        if (this.frames.size == 0) {
            throw new IllegalStateException("No regions found for '" + regionName + "' in Scream.atlas");
        }

        position.set(x, y);
        updateHitbox();
    }

    public void update(float delta) {
        if (phase == Phase.FINISHED) return;

        hitboxTicksElapsed++;

        frameTimer += delta;
        if (frameTimer >= FRAME_DURATION) {
            frameTimer -= FRAME_DURATION;
            if (frameIndex < frames.size - 1) {
                frameIndex++;
            } else {
                // Already showed the last frame for one full duration -> vanish now.
                phase = Phase.FINISHED;
            }
        }

        updateHitbox();
    }

    private void updateHitbox() {
        float halfAngleRad   = (float) Math.toRadians(ANGLE_DEGREES / 2f);
        float baseHalfWidth  = HEIGHT * (float) Math.tan(halfAngleRad);

        float[] vertices = {
            position.x, position.y,                                 // apex
            position.x - baseHalfWidth, position.y + HEIGHT,         // base left
            position.x + baseHalfWidth, position.y + HEIGHT          // base right
        };
        hitboxPolygon.setVertices(vertices);
    }

    /** True while the (short-lived) damage hitbox should still be checked. */
    public boolean isHitboxActive() { return hitboxTicksElapsed < HITBOX_LIFETIME_TICKS; }

    public boolean hasHit(Damageable target) { return hitTargets.contains(target, true); }

    public void registerHit(Damageable target) { hitTargets.add(target); }

    public TextureRegion getFrame()          { return frames.get(frameIndex); }
    public Polygon        getHitboxPolygon()  { return hitboxPolygon; }
    public Vector2         getPosition()       { return position; }
    public Phase           getPhase()          { return phase; }
    public boolean         isFinished()        { return phase == Phase.FINISHED; }
    public boolean         isVoidHeartActive()  { return voidHeartActive; }
    public float           getDamage()         { return damage; }

    public void render(SpriteBatch batch) {
        TextureRegion frame = getFrame();
        float w = frame.getRegionWidth();
        float h = frame.getRegionHeight();
        // Anchored so the bottom-center of the sprite sits at the cast position.
        batch.draw(frame, position.x - w / 2f, position.y, w, h);
    }
}
