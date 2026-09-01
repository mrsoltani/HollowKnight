package com.Graphic.utils;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;

/**
 * Shared "damage flash" renderer.
 *
 * Draws a frame twice: once with green and blue knocked down so the sprite itself
 * reads red, then once more with additive blending for a glow that fades out over
 * the remaining flash time.
 *
 * Every damageable actor (regular enemies and bosses) routes its draw through here
 * so a hit looks identical no matter what it is you just struck.
 *
 * Negative width/height is passed straight through, so callers that flip by drawing
 * with a negative size keep working unchanged.
 */
public final class DamageFlash {

    /** How long a hit flash lasts, in seconds. */
    public static final float DURATION = 0.25f;

    /** Green/blue multiplier at the very start of the flash. 1 = untinted, 0 = pure red. */
    private static final float TINT_FLOOR = 0.20f;

    /** Strength of the additive glow at the very start of the flash. */
    private static final float GLOW_ALPHA = 0.55f;
    private static final float GLOW_R     = 1.00f;
    private static final float GLOW_G     = 0.12f;
    private static final float GLOW_B     = 0.12f;

    private DamageFlash() {}

    /**
     * @param remaining seconds left on this actor's flash timer; anything <= 0 draws untinted.
     */
    public static void draw(SpriteBatch batch, TextureRegion frame,
                            float x, float y, float w, float h,
                            float remaining) {
        if (frame == null) return;

        if (remaining <= 0f) {
            batch.draw(frame, x, y, w, h);
            return;
        }

        float t = MathUtils.clamp(remaining / DURATION, 0f, 1f);

        // Pass 1 — desaturate toward red. Multiplicative, so the sprite never brightens.
        float gb = MathUtils.lerp(1f, TINT_FLOOR, t);
        batch.setColor(1f, gb, gb, 1f);
        batch.draw(frame, x, y, w, h);

        // Pass 2 — additive red glow, fading with the timer. Texture alpha keeps the
        // glow inside the sprite silhouette instead of drawing a solid quad.
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        batch.setColor(GLOW_R, GLOW_G, GLOW_B, GLOW_ALPHA * t);
        batch.draw(frame, x, y, w, h);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        batch.setColor(Color.WHITE);
    }
}
