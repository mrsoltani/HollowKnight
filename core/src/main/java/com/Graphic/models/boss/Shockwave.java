package com.Graphic.models.boss;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

public class Shockwave {


    private static final float SPEED           = 900f;
    private static final float LIFETIME        = 1.6f;
    private static final float SCALE           = 3.0f;
    private static final float HITBOX_WIDTH    = 40f * SCALE;
    private static final float FRAME_DURATION  = 1f / 14f;


    private static TextureAtlas atlas;
    private static Animation<TextureRegion> animation;
    private static float frameWidth  = 0f;
    private static float frameHeight = 0f;

    private static void ensureLoaded() {
        if (atlas != null) return;
        atlas = new TextureAtlas(Gdx.files.internal("sprites/shockwave/shockwave.atlas"));
        Array<TextureAtlas.AtlasRegion> regions = atlas.findRegions("Shockwave");
        TextureRegion[] frames = new TextureRegion[regions.size];
        for (int i = 0; i < regions.size; i++) frames[i] = regions.get(i);

        animation = new Animation<>(FRAME_DURATION, frames);
        animation.setPlayMode(Animation.PlayMode.NORMAL);

        if (frames.length > 0) {
            frameWidth  = frames[0].getRegionWidth();
            frameHeight = frames[0].getRegionHeight();
        }
    }

    public static void disposeShared() {
        if (atlas != null) {
            atlas.dispose();
            atlas = null;
            animation = null;
        }
    }


    private float x, y;
    private float stateTime = 0f;
    private final boolean movingRight;
    private boolean alive = true;

    private final Rectangle hitbox = new Rectangle();

    public Shockwave(float spawnCenterX, float groundY, boolean movingRight) {
        ensureLoaded();
        this.movingRight = movingRight;

        this.x = spawnCenterX - (frameWidth * SCALE) / 2f;
        this.y = groundY;
    }

    public void update(float delta, Rectangle roomBounds) {
        if (!alive) return;

        stateTime += delta;
        x += (movingRight ? SPEED : -SPEED) * delta;

        updateHitbox();

        if (stateTime > LIFETIME) {
            alive = false;
            return;
        }
        if (roomBounds != null) {

            if (x + (frameWidth * SCALE) < roomBounds.x || x > roomBounds.x + roomBounds.width) {
                alive = false;
            }
        }
    }

    private void updateHitbox() {
        float scaledWidth = frameWidth * SCALE;
        float scaledHeight = frameHeight * SCALE;


        float hbX = movingRight ? (x + scaledWidth - HITBOX_WIDTH) : x;
        hitbox.set(hbX, y, HITBOX_WIDTH, scaledHeight);
    }

    public boolean isAlive() { return alive; }

    public Rectangle getHitbox() { return hitbox; }

    public void render(SpriteBatch batch) {
        TextureRegion frame = animation.getKeyFrame(stateTime, false);
        if (frame == null) return;

        float scaledWidth = frameWidth * SCALE;
        float scaledHeight = frameHeight * SCALE;

        if (movingRight) {
            batch.draw(frame, x, y, scaledWidth, scaledHeight);
        } else {

            batch.draw(frame, x + scaledWidth, y, -scaledWidth, scaledHeight);
        }
    }

    public void drawDebug(ShapeRenderer sr) {
        sr.rect(hitbox.x, hitbox.y, hitbox.width, hitbox.height);
    }
}
