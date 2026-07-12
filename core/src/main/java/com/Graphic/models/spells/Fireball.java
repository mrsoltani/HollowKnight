package com.Graphic.models.spells;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;


public class Fireball {

    public enum Phase { TRAVELING, VANISHING, FINISHED }


    public static float HITBOX_WIDTH  = 30f;
    public static float HITBOX_HEIGHT = 70f;


    public static float SPEED          = 3500f;
    public static float FRAME_DURATION = 1f / 14f;


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



            if (frameIndex < frames.size - 1) {
                frameTimer += delta;
                while (frameTimer >= FRAME_DURATION && frameIndex < frames.size - 1) {
                    frameTimer -= FRAME_DURATION;
                    frameIndex++;
                }
            }
        } else {
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


        float frontEdgeX = facingRight ? position.x + imgHalfWidth : position.x - imgHalfWidth;
        float x = facingRight ? frontEdgeX : frontEdgeX - HITBOX_WIDTH;

        hitbox.set(x, position.y - HITBOX_HEIGHT / 2f, HITBOX_WIDTH, HITBOX_HEIGHT);
    }


    public void hitWall() {
        if (phase == Phase.TRAVELING) {
            FRAME_DURATION/=4;
            phase      = Phase.VANISHING;
            frameTimer = 0f;
        }
    }


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
