package com.Graphic.views.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;

import java.util.Comparator;

public class HealthMaskAssets {

    private static final float REFILL_FRAME_DURATION = 1f / 12f;
    private static final float SHINE_FRAME_DURATION = 1f / 10f;
    private static final float BREAK_FRAME_DURATION = 1f / 12f;

    final TextureAtlas atlas;
    final AtlasRegion emptyRegion;
    final AtlasRegion filledRegion;
    final Animation<AtlasRegion> refillAnimation;
    final Animation<AtlasRegion> shineAnimation;
    final Animation<AtlasRegion> breakAnimation;
    final float originalWidth;
    final float originalHeight;

    public HealthMaskAssets() {
        atlas = new TextureAtlas(Gdx.files.internal("ui/mask/mask.atlas"));

        emptyRegion = atlas.findRegion("EmptyHealth");
        filledRegion = atlas.findRegion("FilledHealth");

        if (emptyRegion == null || filledRegion == null) {
            throw new GdxRuntimeException("Missing expected static mask region(s) — check atlas region names.");
        }

        refillAnimation = buildAnimation("HealthRefill", REFILL_FRAME_DURATION, Animation.PlayMode.NORMAL);
        shineAnimation = buildAnimation("FilledHealthShine", SHINE_FRAME_DURATION, Animation.PlayMode.LOOP);
        breakAnimation = buildAnimation("BreakHealth", BREAK_FRAME_DURATION, Animation.PlayMode.NORMAL);

        originalWidth = emptyRegion.originalWidth;
        originalHeight = emptyRegion.originalHeight;
    }

    private Animation<AtlasRegion> buildAnimation(String name, float frameDuration, Animation.PlayMode mode) {
        Array<AtlasRegion> regions = atlas.findRegions(name);
        if (regions == null || regions.size == 0) {
            throw new GdxRuntimeException("Missing expected mask animation group: " + name);
        }
        Array<AtlasRegion> sorted = new Array<>(regions);
        sorted.sort(Comparator.comparingInt(r -> r.index));
        return new Animation<>(frameDuration, sorted, mode);
    }

    public void dispose() {
        atlas.dispose();
    }
}
