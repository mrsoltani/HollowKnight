package com.Graphic.views.ui;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;

public class HealthMask {

    public enum State { EMPTY, FILLING, COMPLETE, BREAKING }

    private final HealthMaskAssets assets;
    private final float displayHeight;

    private State state = State.EMPTY;
    private float stateTime = 0f;

    private float x, y;

    public HealthMask(HealthMaskAssets assets, float displayHeight) {
        this.assets = assets;
        this.displayHeight = displayHeight;
    }

    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public float getDisplayWidth() {
        return displayHeight * (assets.originalWidth / assets.originalHeight);
    }

    public State getState() {
        return state;
    }

    public void fill() {
        if (state == State.COMPLETE || state == State.FILLING) return;
        state = State.FILLING;
        stateTime = 0f;
    }

    public void empty() {
        if (state == State.EMPTY || state == State.BREAKING) return;
        state = State.BREAKING;
        stateTime = 0f;
    }

    public void setFilledInstant(boolean filled) {
        state = filled ? State.COMPLETE : State.EMPTY;
        stateTime = 0f;
    }

    public void update(float delta) {
        stateTime += delta;
        if (state == State.FILLING && assets.refillAnimation.isAnimationFinished(stateTime)) {
            state = State.COMPLETE;
            stateTime = 0f;
        } else if (state == State.BREAKING && assets.breakAnimation.isAnimationFinished(stateTime)) {
            state = State.EMPTY;
            stateTime = 0f;
        }
    }

    public void render(SpriteBatch batch) {
        AtlasRegion region;
        switch (state) {
            case FILLING:
                region = assets.refillAnimation.getKeyFrame(stateTime);
                break;
            case COMPLETE:
                region = assets.shineAnimation.getKeyFrame(stateTime);
                break;
            case BREAKING:
                region = assets.breakAnimation.getKeyFrame(stateTime);
                break;
            case EMPTY:
            default:
                region = assets.emptyRegion;
                break;
        }

        float scale = displayHeight / assets.originalHeight;
        float drawX = x + region.offsetX * scale;
        float drawY = y + region.offsetY * scale;
        float drawW = region.packedWidth * scale;
        float drawH = region.packedHeight * scale;

        batch.draw(region, drawX, drawY, drawW, drawH);
    }
}
