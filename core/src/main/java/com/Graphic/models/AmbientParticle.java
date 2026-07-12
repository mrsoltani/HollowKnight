package com.Graphic.models;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;

public class AmbientParticle {
    private float x, y;
    private float velocityX, velocityY;
    private float baseScale;
    private float currentScale;
    private float maxLifetime;
    private float currentLifetime;
    private float randomAlpha; // Unique opacity for this specific particle

    public AmbientParticle() {}

    public void init(float minX, float maxX, float minY, float maxY, float minScale, float maxScale) {
        this.x = MathUtils.random(minX, maxX);
        this.y = MathUtils.random(minY, maxY);

        this.velocityX = MathUtils.random(-15f, 15f);
        this.velocityY = MathUtils.random(-5f, 20f);

        this.baseScale = MathUtils.random(minScale, maxScale);
        this.currentScale = this.baseScale;

        this.maxLifetime = MathUtils.random(2.0f, 4.5f);
        this.currentLifetime = 0f;

        // Assign a random opacity value between 0.3f (faint) and 1.0f (fully bright)
        this.randomAlpha = MathUtils.random(0.3f, 1.0f);
    }

    public void update(float delta) {
        currentLifetime += delta;

        x += velocityX * delta;
        y += velocityY * delta;

        // Shrink linearly to 0 scale over time
        float lifeRatio = currentLifetime / maxLifetime;
        currentScale = baseScale * (1f - lifeRatio);

        if (currentScale < 0f) currentScale = 0f;
    }

    public void draw(SpriteBatch batch, TextureRegion texture) {
        if (currentScale <= 0f) return;

        // Apply the pre-calculated random opacity to the color mask
        batch.setColor(1f, 1f, 1f, randomAlpha);

        batch.draw(texture,
            x - texture.getRegionWidth() / 2f,
            y - texture.getRegionHeight() / 2f,
            texture.getRegionWidth() / 2f,
            texture.getRegionHeight() / 2f,
            texture.getRegionWidth(),
            texture.getRegionHeight(),
            currentScale, currentScale, 0f
        );
    }

    public boolean isDead() {
        return currentLifetime >= maxLifetime;
    }
}
