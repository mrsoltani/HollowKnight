package com.Graphic.models.enemies;

import com.Graphic.managers.EventBus;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;


public class Mosquito extends BaseEnemy {


    private static final int FRAME_W = 220;
    private static final int FRAME_H = 155;


    public static float HITBOX_WIDTH  = 90f;
    public static float HITBOX_HEIGHT = 70f;
    public static float SPRITE_OFFSET_X = 0f;
    public static float SPRITE_OFFSET_Y = -30f;

    private static final float CONTACT_DAMAGE   = 1f;
    private static final float MAX_HEALTH       = 25f;

    private static final float VISION_WIDTH  = 520f;
    private static final float VISION_HEIGHT = 320f;

    private static final float IDLE_FLY_SPEED       = 55f;
    private static final float IDLE_FOLLOW_DEADZONE = 70f;
    private static final float IDLE_MIN_TIME        = 0.7f;

    private static final float ANTICIPATE_TIME = 0.45f;
    private static final float SWOOP_START_SPEED = 160f;
    private static final float SWOOP_ACCEL       = 900f;
    private static final float SWOOP_MAX_SPEED   = 900f;



    private static Rectangle globalMapBounds = null;

    private final Rectangle visionBox = new Rectangle();
    private final Vector2 swoopTarget = new Vector2();
    private final Vector2 swoopDir    = new Vector2();

    private float anticipateTimer = 0f;
    private float swoopSpeed      = 0f;
    private float idleTimer       = 0f;

    public Mosquito(float x, float y, boolean facingRight) {
        super(x, y, HITBOX_WIDTH, HITBOX_HEIGHT, facingRight, MAX_HEALTH, false);
        setSpriteBox(FRAME_W, FRAME_H, SPRITE_OFFSET_X, SPRITE_OFFSET_Y);

        anim.register(EnemyState.IDLE, "enemies/Mosquito/Idle.png", FRAME_W, FRAME_H, 0.08f, Animation.PlayMode.LOOP);
        anim.register(EnemyState.TURN, "enemies/Mosquito/Turn.png", FRAME_W, FRAME_H, 0.05f, Animation.PlayMode.NORMAL);
        anim.register(EnemyState.ATTACK_ANTICIPATE, "enemies/Mosquito/Attack Anticipate.png", FRAME_W, FRAME_H, 0.10f, Animation.PlayMode.NORMAL);
        anim.register(EnemyState.ATTACK, "enemies/Mosquito/Attack.png", FRAME_W, FRAME_H, 0.05f, Animation.PlayMode.NORMAL);
        anim.register(EnemyState.DEAD_AIR, "enemies/Mosquito/Death_Air.png", FRAME_W, FRAME_H, 0.10f, Animation.PlayMode.NORMAL);
        anim.register(EnemyState.DEAD_LAND, "enemies/Mosquito/Death_Land.png", FRAME_W, FRAME_H, 0.15f, Animation.PlayMode.NORMAL);

        changeState(EnemyState.IDLE);
    }


    public static void setMapBounds(Rectangle bounds) {
        globalMapBounds = bounds;
    }

    private void updateVisionBox() {
        float centerX = bounds.x + bounds.width / 2f;
        float centerY = bounds.y + bounds.height / 2f;
        float forwardBias = facingRight ? VISION_WIDTH * 0.15f : -VISION_WIDTH * 0.15f;
        visionBox.set(
            centerX + forwardBias - VISION_WIDTH / 2f,
            centerY - VISION_HEIGHT / 2f,
            VISION_WIDTH, VISION_HEIGHT);
    }

    @Override
    protected void updateAI(float delta, Rectangle target, Array<Rectangle> platforms) {
        updateVisionBox();

        switch (state) {
            case IDLE:
                idleTimer += delta;
                if (target != null) {
                    float pcx = target.x + target.width / 2f;
                    float pcy = target.y + target.height / 2f;
                    float mcx = bounds.x + bounds.width / 2f;
                    float mcy = bounds.y + bounds.height / 2f;
                    float dx = pcx - mcx, dy = pcy - mcy;
                    float dist = (float) Math.sqrt(dx * dx + dy * dy);

                    if ((pcx > mcx) != facingRight) faceRight(pcx > mcx);
                    if (dist > IDLE_FOLLOW_DEADZONE) {
                        velocity.set(dx / dist * IDLE_FLY_SPEED, dy / dist * IDLE_FLY_SPEED);
                    } else {
                        velocity.set(0, 0);
                    }

                    if (idleTimer >= IDLE_MIN_TIME && visionBox.overlaps(target)) {
                        beginAnticipation(target);
                    }
                } else {
                    velocity.set(0, 0);
                }
                break;

            case ATTACK_ANTICIPATE:
                velocity.set(0, 0);
                anticipateTimer += delta;
                if (anticipateTimer >= ANTICIPATE_TIME) {
                    beginSwoop();
                }
                break;

            case ATTACK:
                swoopSpeed = Math.min(swoopSpeed + SWOOP_ACCEL * delta, SWOOP_MAX_SPEED);
                velocity.set(swoopDir).scl(swoopSpeed);

                boolean hitMapEdge = false;
                if (globalMapBounds != null) {
                    float nextX = bounds.x + velocity.x * delta;
                    float nextY = bounds.y + velocity.y * delta;

                    if (nextX <= globalMapBounds.x || nextX + bounds.width >= globalMapBounds.x + globalMapBounds.width ||
                        nextY <= globalMapBounds.y || nextY + bounds.height >= globalMapBounds.y + globalMapBounds.height) {
                        hitMapEdge = true;
                    }
                }

                if (isBlockedBySolid() || hitMapEdge) {
                    velocity.set(0, 0);
                    swoopSpeed = 0f;
                    idleTimer = 0f;
                    changeState(EnemyState.IDLE);
                }
                break;

            case TURN:
                velocity.set(0, 0);
                if (anim.isFinished(EnemyState.TURN)) {
                    faceRight(!facingRight);
                    idleTimer = 0f;
                    changeState(EnemyState.IDLE);
                }
                break;

            default:
                break;
        }


        if (globalMapBounds != null) {
            float nextX = bounds.x + velocity.x * delta;
            float nextY = bounds.y + velocity.y * delta;


            if (nextX < globalMapBounds.x) {
                bounds.x = globalMapBounds.x;
                velocity.x = 0;
            } else if (nextX + bounds.width > globalMapBounds.x + globalMapBounds.width) {
                bounds.x = globalMapBounds.x + globalMapBounds.width - bounds.width;
                velocity.x = 0;
            }


            if (nextY < globalMapBounds.y) {
                bounds.y = globalMapBounds.y;
                velocity.y = 0;
            } else if (nextY + bounds.height > globalMapBounds.y + globalMapBounds.height) {
                bounds.y = globalMapBounds.y + globalMapBounds.height - bounds.height;
                velocity.y = 0;
            }
        }
    }

    private void beginAnticipation(Rectangle target) {
        anticipateTimer = 0f;
        swoopTarget.set(target.x + target.width / 2f, target.y + target.height / 2f);
        faceRight(swoopTarget.x > bounds.x + bounds.width / 2f);
        changeState(EnemyState.ATTACK_ANTICIPATE);
    }

    private void beginSwoop() {
        swoopSpeed = SWOOP_START_SPEED;
        Vector2 center = bounds.getCenter(new Vector2());
        swoopDir.set(swoopTarget).sub(center);
        if (swoopDir.isZero(0.001f)) {
            swoopDir.set(facingRight ? 1f : -1f, 0f);
        }
        swoopDir.nor();
        changeState(EnemyState.ATTACK);
        EventBus.emit(EventBus.Event.ENEMY_MOSQUITO_ATTACK, enemyTypeName());
    }

    @Override
    protected float getContactDamage() {
        return CONTACT_DAMAGE;
    }

    @Override
    protected String enemyTypeName() {
        return "mosquito";
    }

    @Override
    public void renderDebug(ShapeRenderer shapes) {
        super.renderDebug(shapes);

        if (!isDying()) {
            shapes.setColor(Color.YELLOW);
            shapes.rect(visionBox.x, visionBox.y, visionBox.width, visionBox.height);

            if (state == EnemyState.ATTACK_ANTICIPATE || state == EnemyState.ATTACK) {
                shapes.setColor(Color.MAGENTA);
                shapes.line(swoopTarget.x - 8, swoopTarget.y, swoopTarget.x + 8, swoopTarget.y);
                shapes.line(swoopTarget.x, swoopTarget.y - 8, swoopTarget.x, swoopTarget.y + 8);
            }
        }
    }

    public Rectangle getVisionBox() {
        return visionBox;
    }
}
