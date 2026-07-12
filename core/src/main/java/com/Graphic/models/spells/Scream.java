package com.Graphic.models.spells;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;


public class Scream {

    public enum Phase { ACTIVE, FINISHED }


    public static float HEIGHT        = 200f;
    public static float ANGLE_DEGREES = 60f;


    public static float FRAME_DURATION       = 1f / 15f;
    public static int   HITBOX_LIFETIME_TICKS = 3;


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

                phase = Phase.FINISHED;
            }
        }

        updateHitbox();
    }

    private void updateHitbox() {
        float halfAngleRad   = (float) Math.toRadians(ANGLE_DEGREES / 2f);
        float baseHalfWidth  = HEIGHT * (float) Math.tan(halfAngleRad);

        float[] vertices = {
            position.x, position.y,
            position.x - baseHalfWidth, position.y + HEIGHT,
            position.x + baseHalfWidth, position.y + HEIGHT
        };
        hitboxPolygon.setVertices(vertices);
    }


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

        batch.draw(frame, position.x - w / 2f, position.y, w, h);
    }
}
