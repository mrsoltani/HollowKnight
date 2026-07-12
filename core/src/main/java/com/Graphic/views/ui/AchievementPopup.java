package com.Graphic.views.ui;

import com.Graphic.managers.EventBus;
import com.Graphic.managers.FontManager;
import com.Graphic.managers.LocalizationManager;
import com.Graphic.models.Achievement;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Array;

public class AchievementPopup {

    private static final float DELAY_DURATION   = 2.0f; // 2-second delay before showing
    private static final float DISPLAY_DURATION = 3.5f; // Total visible time
    private static final float SLIDE_DURATION   = 0.4f;
    private static final float ICON_SIZE        = 64f;

    // Position offsets from the bottom-left corner
    private static final float MARGIN_LEFT      = 30f;
    private static final float MARGIN_BOTTOM    = 30f;

    private static final Color TITLE_COLOR      = new Color(1f, 0.85f, 0.35f, 1f);

    private final Array<Achievement> queue = new Array<>();
    private Achievement current;
    private float timer;

    public AchievementPopup() {
        EventBus.subscribe(EventBus.Event.ACHIEVEMENT_UNLOCKED, data -> {
            if (data instanceof Achievement a) queue.add(a);
        });
    }

    public void update(float delta) {
        if (current == null && queue.size > 0) {
            current = queue.removeIndex(0);
            timer = 0f;
        }
        if (current != null) {
            timer += delta;
            // Life cycle ends after the delay period + the display period are over
            if (timer >= DELAY_DURATION + DISPLAY_DURATION) current = null;
        }
    }

    public void render(SpriteBatch batch) {
        // Do not render anything if no item is active, or if we are still waiting out the delay
        if (current == null || timer < DELAY_DURATION) return;

        // Calculate active time relative to when it actually started showing
        float activeTimer = timer - DELAY_DURATION;
        float fadeOutStart = DISPLAY_DURATION - SLIDE_DURATION;

        float alpha = activeTimer < SLIDE_DURATION
            ? activeTimer / SLIDE_DURATION
            : (activeTimer > fadeOutStart ? 1f - (activeTimer - fadeOutStart) / SLIDE_DURATION : 1f);

        // Updated for Bottom-Left Positioning
        float x = MARGIN_LEFT;
        float y = MARGIN_BOTTOM;

        batch.setColor(1f, 1f, 1f, alpha);
        batch.draw(current.getIcon(), x, y, ICON_SIZE, ICON_SIZE);

        FontManager.getMenuSmall().setColor(TITLE_COLOR.r, TITLE_COLOR.g, TITLE_COLOR.b, alpha);
        String title = LocalizationManager.get(current.getTitleKey());
        FontManager.getMenuSmall().draw(batch, title, x + ICON_SIZE + 16f, y + ICON_SIZE * 0.6f);

        // Reset text and drawing batch color channels
        batch.setColor(1f, 1f, 1f, 1f);
        FontManager.getMenuSmall().setColor(1f, 1f, 1f, 1f);
    }
}
