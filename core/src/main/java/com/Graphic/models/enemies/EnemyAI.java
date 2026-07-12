package com.Graphic.models.enemies;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

/**
 * Behavioural contract shared by every enemy.
 *
 * The screen (or, later, the real game loop) only ever talks to enemies through
 * this interface: tick their brain, draw their sprite, and — for debugging —
 * draw their boxes. Concrete enemies additionally implement
 * {@link com.Graphic.models.spells.Damageable} so the existing spell system can
 * damage them with no extra wiring.
 */
public interface EnemyAI {

    /**
     * Advance the enemy's state machine and physics by {@code delta} seconds.
     *
     * @param delta      frame time in seconds
     * @param target     the thing this enemy hunts / can damage (the player).
     *                   Passed as a raw {@link Rectangle} so enemies are not
     *                   coupled to any concrete Player type.
     * @param platforms  solid world geometry used for gravity, wall and
     *                   cliff-edge sensing.
     */
    void update(float delta, Rectangle target, Array<Rectangle> platforms);

    /** Draw the current animation frame. Call between batch.begin()/end(). */
    void render(SpriteBatch batch);

    /**
     * Draw debug shapes (hitbox + vision box). Call between a
     * {@link ShapeRenderer} begin(Line)/end() pass.
     */
    void renderDebug(ShapeRenderer shapes);

    /** The damage-dealing hitbox (also the physics body). */
    Rectangle getHitbox();

    /** True once the enemy has fully died (terminal DEAD_LAND clip finished). */
    boolean isDead();

    /** Free any GPU resources this enemy owns. */
    void dispose();
}
