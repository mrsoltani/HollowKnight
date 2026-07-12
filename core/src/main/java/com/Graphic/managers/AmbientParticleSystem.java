package com.Graphic.managers;

import com.Graphic.models.AmbientParticle;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;

public class AmbientParticleSystem {
    private final Array<AmbientParticle> particles;
    private final TextureRegion particleTexture;
    private final int maxParticles;

    private float minScale;
    private float maxScale;

    public AmbientParticleSystem(TextureRegion texture, int maxParticles, float minScale, float maxScale) {
        this.particleTexture = texture;
        this.maxParticles = maxParticles;
        this.minScale = minScale;
        this.maxScale = maxScale;
        this.particles = new Array<>(false, maxParticles);
    }

    /** Pre-populates the room immediately so particles don't all visibly pop in when the room loads */
    public void populateRegion(float mapWidth, float mapHeight) {
        particles.clear();
        for (int i = 0; i < maxParticles; i++) {
            AmbientParticle p = new AmbientParticle();
            p.init(0, mapWidth, 0, mapHeight, minScale, maxScale);
            // Fast-forward lifetime randomly so they are in varied stages of life/fading
            float artificialAge = com.badlogic.gdx.math.MathUtils.random(0f, 2f);
            p.update(artificialAge);
            if (!p.isDead()) {
                particles.add(p);
            }
        }
    }

    public void update(float delta, float mapWidth, float mapHeight) {
        // Update and filter out dead particles
        for (int i = particles.size - 1; i >= 0; i--) {
            AmbientParticle p = particles.get(i);
            p.update(delta);
            if (p.isDead()) {
                particles.removeIndex(i);
            }
        }

        // Replenish missing spaces to maintain density
        while (particles.size < maxParticles) {
            AmbientParticle p = new AmbientParticle();
            p.init(0, mapWidth, 0, mapHeight, minScale, maxScale);
            particles.add(p);
        }
    }

    public void draw(SpriteBatch batch) {
        for (AmbientParticle p : particles) {
            p.draw(batch, particleTexture);
        }
        // Always restore batch configuration safety defaults
        batch.setColor(1f, 1f, 1f, 1f);
    }
}
