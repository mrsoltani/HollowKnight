package com.Graphic.models.enemies;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

/**
 * Enemy 1 — Crystal Crawler (ground patroller).
 *
 * Behaviour: walks in a straight line at constant speed, completely ignoring
 * the player. When it meets a wall or reaches a cliff edge it plays a TURN clip,
 * flips, and continues the other way.
 *
 * States: WALK, TURN, DEAD_AIR, DEAD_LAND.
 *
 * Frame data (from reference): 155 x 180.
 *   WALK       enemies/CrystalCrawler/Walk.png       0.10 LOOP
 *   TURN       enemies/CrystalCrawler/Turn.png       0.09 NORMAL
 *   DEAD_AIR   enemies/CrystalCrawler/Death_Air.png  0.10 NORMAL
 *   DEAD_LAND  enemies/CrystalCrawler/Death_Land.png 0.15 NORMAL
 */

public class CrystalCrawler extends BaseEnemy {

    private static final int FRAME_W = 155;
    private static final int FRAME_H = 180;

    // ── Hitbox (TUNE THESE) — a tight body box, smaller than the 155x180 frame,
    //    so the transparent margins around the crawler don't deal contact damage.
    //    The sprite is drawn full-size over this via setSpriteBox below.
    public static float HITBOX_WIDTH   = 110f;
    public static float HITBOX_HEIGHT  = 70f;
    public static float SPRITE_OFFSET_X = 0f;
    public static float SPRITE_OFFSET_Y = 0f; // feet already sit at the hitbox floor

    private static final float WALK_SPEED     = 55f;  // px/s constant patrol
    private static final float CONTACT_DAMAGE = 1f;
    private static final float SENSOR_LOOKAHEAD = 6f; // px probe for wall/cliff
    private static final float MAX_HEALTH     = 3f;

    public CrystalCrawler(float x, float y, boolean facingRight) {
        // Ground enemy -> affected by gravity while alive. Body = tuned hitbox.
        super(x, y, HITBOX_WIDTH, HITBOX_HEIGHT, facingRight, MAX_HEALTH, true);

        // Draw the full-size sprite over the smaller hitbox.
        setSpriteBox(FRAME_W, FRAME_H, SPRITE_OFFSET_X, SPRITE_OFFSET_Y);

        anim.register(EnemyState.WALK,
            "enemies/CrystalCrawler/Walk.png", FRAME_W, FRAME_H, 0.10f, Animation.PlayMode.LOOP);
        anim.register(EnemyState.TURN,
            "enemies/CrystalCrawler/Turn.png", FRAME_W, FRAME_H, 0.09f, Animation.PlayMode.NORMAL);
        anim.register(EnemyState.DEAD_AIR,
            "enemies/CrystalCrawler/Death_Air.png", FRAME_W, FRAME_H, 0.10f, Animation.PlayMode.NORMAL);
        anim.register(EnemyState.DEAD_LAND,
            "enemies/CrystalCrawler/Death_Land.png", FRAME_W, FRAME_H, 0.15f, Animation.PlayMode.NORMAL);

        changeState(EnemyState.WALK);
    }

    @Override
    protected void updateAI(float delta, Rectangle target, Array<Rectangle> platforms) {
        switch (state) {
            case WALK:
                // Constant-speed patrol in the facing direction.
                velocity.x = facingRight ? WALK_SPEED : -WALK_SPEED;

                // Turn if we're about to hit a wall or walk off a cliff.
                boolean wall  = isWallAhead(platforms, SENSOR_LOOKAHEAD) || isBlockedByWall();
                boolean cliff = onGround && isCliffAhead(platforms, SENSOR_LOOKAHEAD);
                if (wall || cliff) {
                    velocity.x = 0;
                    changeState(EnemyState.TURN);
                }
                break;

            case TURN:
                // Stand still through the turn clip, then flip and resume.
                velocity.x = 0;
                if (anim.isFinished(EnemyState.TURN)) {
                    faceRight(!facingRight);
                    changeState(EnemyState.WALK);
                }
                break;

            default:
                break;
        }
    }

    @Override
    protected float getContactDamage() {
        return CONTACT_DAMAGE;
    }

    @Override
    protected String enemyTypeName() {
        return "crystalcrawler";
    }
}
