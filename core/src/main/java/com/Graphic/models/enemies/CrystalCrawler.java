package com.Graphic.models.enemies;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;



public class CrystalCrawler extends BaseEnemy {

    private static final int FRAME_W = 155;
    private static final int FRAME_H = 180;




    public static float HITBOX_WIDTH   = 110f;
    public static float HITBOX_HEIGHT  = 70f;
    public static float SPRITE_OFFSET_X = 0f;
    public static float SPRITE_OFFSET_Y = 0f;

    private static final float WALK_SPEED     = 55f;
    private static final float CONTACT_DAMAGE = 1f;
    private static final float SENSOR_LOOKAHEAD = 6f;
    private static final float MAX_HEALTH     = 40f;

    public CrystalCrawler(float x, float y, boolean facingRight) {

        super(x, y, HITBOX_WIDTH, HITBOX_HEIGHT, facingRight, MAX_HEALTH, true);


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

                velocity.x = facingRight ? WALK_SPEED : -WALK_SPEED;


                boolean wall  = isWallAhead(platforms, SENSOR_LOOKAHEAD) || isBlockedByWall();
                boolean cliff = onGround && isCliffAhead(platforms, SENSOR_LOOKAHEAD);
                if (wall || cliff) {
                    velocity.x = 0;
                    changeState(EnemyState.TURN);
                }
                break;

            case TURN:

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
