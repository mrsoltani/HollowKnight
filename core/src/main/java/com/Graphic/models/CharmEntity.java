package com.Graphic.models;


import com.Graphic.managers.*;
import com.Graphic.models.charms.CharmId;
import com.Graphic.utils.CharmSpawnData;
import com.Graphic.views.ui.GameHUD;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

public class CharmEntity {

    private final TextureAtlas atlas;
    private final Animation<TextureRegion> animation;
    private float stateTime = 0f;

    private final Vector2 animPos;
    private final Rectangle triggerBox;
    private boolean acquired = false;

    public CharmEntity(CharmSpawnData data) {
        this.animPos = data.animPos;
        this.triggerBox = data.triggerBox;

        atlas = new TextureAtlas(Gdx.files.internal("sprites/objects/glowing_object.atlas"));
        Array<TextureAtlas.AtlasRegion> regions = new Array<>();

        for (int i = 3; i <= 15; i++) {
            String frameName = String.format("ground_plink_smaller%04d", i);
            TextureAtlas.AtlasRegion region = atlas.findRegion(frameName);
            if (region != null) regions.add(region);
        }

        if (regions.size == 0) {
            Gdx.app.error("CharmEntity", "CRITICAL: Sequential frame loader failed!");
            animation = null;
        } else {
            animation = new Animation<>(1f / 12f, regions, Animation.PlayMode.LOOP);
        }
    }

    public void update(float delta, Player player, GameHUD hud) {
        if (acquired || triggerBox == null) return;

        stateTime += delta;
        if (player.getBounds().overlaps(triggerBox)) {
            CharmManager.unlockVoidHeart();
            acquired = true;
            hud.triggerCharmCutscene();
            AudioManager.playSFX("charm_pickup");
            AudioManager.playSFX("charm_pickup_2");
            EventBus.emit(EventBus.Event.CHARM_COLLECTED, CharmId.VOID_HEART);
            // player.acquireCharm(); // Call your player method here
        }
    }

    public void render(SpriteBatch batch) {
        if (acquired || animation == null || animPos == null) return;

        TextureRegion frame = animation.getKeyFrame(stateTime);
        float drawX = animPos.x - frame.getRegionWidth() / 2f;
        float drawY = animPos.y - frame.getRegionHeight() / 2f;
        batch.draw(frame, drawX, drawY);
    }

    public void dispose() {
        if (atlas != null) atlas.dispose();
    }
}
