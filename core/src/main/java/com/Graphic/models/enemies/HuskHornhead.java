package com.Graphic.models.enemies;

import com.Graphic.managers.EventBus;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;


public class HuskHornhead extends BaseEnemy {

    private static final int FRAME_W = 239;
    private static final int FRAME_H = 219;

    private static final float WALK_SPEED       = 70f;
    private static final float LUNGE_SPEED      = 420f;
    private static final float CONTACT_DAMAGE   = 2f;
    private static final float MAX_HEALTH       = 50f;
    private static final float SENSOR_LOOKAHEAD = 6f;

    private static final float WALK_DURATION      = 2.5f;
    private static final float REST_DURATION      = 1.5f;
    private static final float ANTICIPATE_TIME    = 0.35f;


    private static final float VISION_WIDTH  = 360f;
    private static final float VISION_HEIGHT = 180f;

    private final Rectangle visionBox = new Rectangle();


    public static float HITBOX_WIDTH    = 90f;
    public static float HITBOX_HEIGHT   = 150f;
    public static float SPRITE_OFFSET_X = 0f;
    public static float SPRITE_OFFSET_Y = 0f;

    private float phaseTimer      = 0f;
    private float anticipateTimer = 0f;

    public HuskHornhead(float x, float y, boolean facingRight) {

        super(x, y, HITBOX_WIDTH, HITBOX_HEIGHT, facingRight, MAX_HEALTH, true);


        setSpriteBox(FRAME_W, FRAME_H, SPRITE_OFFSET_X, SPRITE_OFFSET_Y);

        anim.register(EnemyState.IDLE,
            "enemies/HuskHornhead/Idle.png", FRAME_W, FRAME_H, 0.10f, Animation.PlayMode.LOOP);
        anim.register(EnemyState.WALK,
            "enemies/HuskHornhead/Walk.png", FRAME_W, FRAME_H, 0.08f, Animation.PlayMode.LOOP);
        anim.register(EnemyState.TURN,
            "enemies/HuskHornhead/Turn.png", FRAME_W, FRAME_H, 0.08f, Animation.PlayMode.NORMAL);
        anim.register(EnemyState.ATTACK_ANTICIPATE,
            "enemies/HuskHornhead/Attack Anticipate.png", FRAME_W, FRAME_H, 0.10f, Animation.PlayMode.NORMAL);
        anim.register(EnemyState.ATTACK_LUNGE,
            "enemies/HuskHornhead/Attack Lunge.png", FRAME_W, FRAME_H, 0.05f, Animation.PlayMode.LOOP);
        anim.register(EnemyState.DEAD_AIR,
            "enemies/HuskHornhead/Death Air.png", FRAME_W, FRAME_H, 0.10f, Animation.PlayMode.NORMAL);
        anim.register(EnemyState.DEAD_LAND,
            "enemies/HuskHornhead/Death Land.png", FRAME_W, FRAME_H, 0.10f, Animation.PlayMode.NORMAL);

        changeState(EnemyState.WALK);
    }


    private void updateVisionBox() {
        float centerY = bounds.y + bounds.height / 2f;
        float x = facingRight ? bounds.x + bounds.width
                              : bounds.x - VISION_WIDTH;
        visionBox.set(x, centerY - VISION_HEIGHT / 2f, VISION_WIDTH, VISION_HEIGHT);
    }

    @Override
    protected void updateAI(float delta, Rectangle target, Array<Rectangle> platforms) {
        updateVisionBox();



        boolean canSpot = (state == EnemyState.WALK || state == EnemyState.IDLE);
        if (canSpot && target != null && visionBox.overlaps(target)) {
            anticipateTimer = 0f;
            velocity.x = 0;
            changeState(EnemyState.ATTACK_ANTICIPATE);
            return;
        }

        switch (state) {
            case WALK:
                velocity.x = facingRight ? WALK_SPEED : -WALK_SPEED;
                phaseTimer += delta;

                boolean wall  = isWallAhead(platforms, SENSOR_LOOKAHEAD) || isBlockedByWall();
                boolean cliff = onGround && isCliffAhead(platforms, SENSOR_LOOKAHEAD);
                if (wall || cliff) {
                    velocity.x = 0;
                    changeState(EnemyState.TURN);
                } else if (phaseTimer >= WALK_DURATION) {

                    phaseTimer = 0f;
                    velocity.x = 0;
                    changeState(EnemyState.IDLE);
                }
                break;

            case IDLE:
                velocity.x = 0;
                phaseTimer += delta;
                if (phaseTimer >= REST_DURATION) {
                    phaseTimer = 0f;
                    changeState(EnemyState.WALK);
                }
                break;

            case TURN:
                velocity.x = 0;
                if (anim.isFinished(EnemyState.TURN)) {
                    faceRight(!facingRight);
                    phaseTimer = 0f;
                    changeState(EnemyState.WALK);
                }
                break;

            case ATTACK_ANTICIPATE:
                velocity.x = 0;
                anticipateTimer += delta;
                if (anticipateTimer >= ANTICIPATE_TIME) {
                    changeState(EnemyState.ATTACK_LUNGE);
                }
                break;

            case ATTACK_LUNGE:


                velocity.x = facingRight ? LUNGE_SPEED : -LUNGE_SPEED;

                boolean lungeWall  = isWallAhead(platforms, SENSOR_LOOKAHEAD) || isBlockedByWall();
                boolean lungeCliff = onGround && isCliffAhead(platforms, SENSOR_LOOKAHEAD);
                if (lungeWall || lungeCliff) {
                    velocity.x = 0;
                    phaseTimer = 0f;
                    changeState(EnemyState.IDLE);
                }
                break;

            default:
                break;
        }
    }

    @Override
    protected float groundStepInterval() {
        return state == EnemyState.WALK ? 0.48f : 0f;
    }

    @Override
    protected float getContactDamage() {
        return CONTACT_DAMAGE;
    }

    @Override
    protected String enemyTypeName() {
        return "huskhornhead";
    }

    @Override
    public void renderDebug(ShapeRenderer shapes) {
        super.renderDebug(shapes);
        if (!isDying()) {
            shapes.setColor(Color.YELLOW);
            shapes.rect(visionBox.x, visionBox.y, visionBox.width, visionBox.height);
        }
    }

    public Rectangle getVisionBox() {
        return visionBox;
    }
}
