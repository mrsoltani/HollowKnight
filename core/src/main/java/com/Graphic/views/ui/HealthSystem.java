package com.Graphic.views.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class HealthSystem {

    private static final int DEFAULT_MAX_HEALTH = 5;
    private static final float MASK_DISPLAY_HEIGHT = 110f;
    private static final float MASK_SPACING = -20f;
    private static final float TOP_MARGIN = 24f;
    private static final float LEFT_MARGIN = 140f;

    private final HealthMaskAssets assets;
    private final HealthMask[] masks;
    private final int maxHealth;
    private int currentHealth;

    public HealthSystem() {
        this(DEFAULT_MAX_HEALTH);
    }

    public HealthSystem(int maxHealth) {
        this.assets = new HealthMaskAssets();
        this.maxHealth = maxHealth;
        this.currentHealth = maxHealth;

        masks = new HealthMask[maxHealth];
        for (int i = 0; i < maxHealth; i++) {
            masks[i] = new HealthMask(assets, MASK_DISPLAY_HEIGHT);
            masks[i].setFilledInstant(true);
        }

        layoutMasks();
    }

    private void layoutMasks() {
        float x = LEFT_MARGIN;
        float y = Gdx.graphics.getHeight() - TOP_MARGIN - MASK_DISPLAY_HEIGHT;
        for (HealthMask mask : masks) {
            mask.setPosition(x, y);
            x += mask.getDisplayWidth() + MASK_SPACING;
        }
    }

    public void resize(int width, int height) {
        layoutMasks();
    }

    public void takeDamage(int amount) {
        for (int i = 0; i < amount; i++) {
            int idx = currentHealth - 1;
            if (idx < 0) break;
            masks[idx].empty();
            currentHealth--;
        }
    }

    public void heal(int amount) {
        for (int i = 0; i < amount; i++) {
            if (currentHealth >= maxHealth) break;
            masks[currentHealth].fill();
            currentHealth++;
        }
    }

    public int getCurrentHealth() {
        return currentHealth;
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    public void update(float delta) {
        for (HealthMask mask : masks) {
            mask.update(delta);
        }
    }

    public void render(SpriteBatch batch) {
        for (HealthMask mask : masks) {
            mask.render(batch);
        }
    }

    public void dispose() {
        assets.dispose();
    }
}
