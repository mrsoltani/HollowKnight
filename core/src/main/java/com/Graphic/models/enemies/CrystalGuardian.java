package com.Graphic.models.enemies;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

/**
 * Enemy 4 — Crystal Guardian (sophisticated stationary boss).
 *
 * Behaviour flow (AI ONLY — laser deferred, see note):
 *   IDLE      : stands on its home spot watching a forward vision box.
 *   SHOOT     : player spotted -> play the gun wind-up clip and WAIT it out.
 *               (This is where the laser will later charge + fire.)
 *   WALK      : immediately after SHOOT, become "enraged" and charge/rush at
 *               the player at high speed until the enrage timer ends.
 *   EVADE     : short recovery/dodge beat once enrage ends (a stylistic pause).
 *   WALK      : then walk back home to the original spot...
 *   IDLE      : ...and resume watching.
 * TURN is played whenever the Guardian needs to flip to face a new direction.
 */
public class CrystalGuardian extends BaseEnemy {

    // ── Sprite frame size (the raw artwork dimensions) ───────────────────────
    private static final int FRAME_W = 285;
    private static final int FRAME_H = 189;

    // ── Hitbox size (TUNE THESE) ─────────────────────────────────────────────
    // Much tighter than the sprite frame for fair combat and wall detection.
    public static float HITBOX_WIDTH  = 100f;
    public static float HITBOX_HEIGHT = 140f;

    // Offset to align the drawing of the large sprite over the smaller hitbox.
    public static float SPRITE_OFFSET_X = 0f;
    public static float SPRITE_OFFSET_Y = -15f;

    private static final float CONTACT_DAMAGE   = 2f;
    private static final float MAX_HEALTH       = 12f;
    private static final float ENRAGE_SPEED     = 300f; // charge/rush speed
    private static final float HOME_SPEED       = 160f; // walk-home speed
    private static final float SENSOR_LOOKAHEAD = 6f;
    private static final float HOME_SLACK       = 8f;   // "close enough to home"

    private static final float ENRAGE_DURATION  = 2.0f; // how long the rush lasts

    private static final float VISION_WIDTH  = 460f;
    private static final float VISION_HEIGHT = 220f;

    private final Rectangle visionBox = new Rectangle();

    /** Original spot to return to after enraging. */
    private final float homeX;
    private final boolean homeFacingRight;

    private float enrageTimer = 0f;

    /**
     * When TURN finishes we need to know what to do next. This records the
     * state to enter after the flip completes.
     */
    private EnemyState postTurnState = EnemyState.IDLE;

    public CrystalGuardian(float x, float y, boolean facingRight) {
        // Pass the tight HITBOX dimensions to super, NOT the frame dimensions.
        // Gravity is true (boss stays on the ground).
        super(x, y, HITBOX_WIDTH, HITBOX_HEIGHT, facingRight, MAX_HEALTH, true);

        // Tell the BaseEnemy how to draw the oversized sprite around the tight hitbox
        setSpriteBox(FRAME_W, FRAME_H, SPRITE_OFFSET_X, SPRITE_OFFSET_Y);

        this.homeX = x;
        this.homeFacingRight = facingRight;

        anim.register(EnemyState.IDLE,
            "enemies/Crystalized/Idle.png", FRAME_W, FRAME_H, 0.15f, Animation.PlayMode.LOOP);
        anim.register(EnemyState.WALK,
            "enemies/Crystalized/Run.png", FRAME_W, FRAME_H, 0.06f, Animation.PlayMode.LOOP);
        anim.register(EnemyState.TURN,
            "enemies/Crystalized/Turn.png", FRAME_W, FRAME_H, 0.08f, Animation.PlayMode.NORMAL);
        anim.register(EnemyState.SHOOT,
            "enemies/Crystalized/Shoot.png", FRAME_W, FRAME_H, 0.10f, Animation.PlayMode.NORMAL);
        anim.register(EnemyState.EVADE,
            "enemies/Crystalized/Evade.png", FRAME_W, FRAME_H, 0.08f, Animation.PlayMode.NORMAL);
        anim.register(EnemyState.DEAD_AIR,
            "enemies/Crystalized/Death_Air.png", FRAME_W, FRAME_H, 0.10f, Animation.PlayMode.NORMAL);
        anim.register(EnemyState.DEAD_LAND,
            "enemies/Crystalized/Death_Land.png", FRAME_W, FRAME_H, 0.12f, Animation.PlayMode.NORMAL);

        changeState(EnemyState.IDLE);
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

        switch (state) {
            case IDLE:
                velocity.x = 0;
                // Watch the forward box. Player spotted -> begin the shoot beat.
                if (target != null && visionBox.overlaps(target)) {
                    changeState(EnemyState.SHOOT);
                }
                break;

            case SHOOT:
                // Stand and play the wind-up. NOTE: laser deliberately not fired.
                velocity.x = 0;
                fireLaser(target); // stub — does nothing yet (see class note)
                if (anim.isFinished(EnemyState.SHOOT)) {
                    // Immediately go enraged and charge the player.
                    enrageTimer = 0f;
                    // Face the player before charging; if we must flip, route
                    // through TURN first and resume WALK afterwards.
                    boolean playerOnRight = playerIsOnRight(target);
                    if (playerOnRight != facingRight) {
                        beginTurn(EnemyState.WALK);
                    } else {
                        changeState(EnemyState.WALK);
                    }
                }
                break;

            case WALK:
                // WALK serves double duty: (a) enrage-charge, then (b) walk home.
                if (enrageTimer < ENRAGE_DURATION) {
                    // ── (a) Enraged rush ──
                    enrageTimer += delta;
                    velocity.x = facingRight ? ENRAGE_SPEED : -ENRAGE_SPEED;

                    // Stop rushing early if we slam a wall or hit a cliff.
                    boolean wall  = isWallAhead(platforms, SENSOR_LOOKAHEAD) || isBlockedByWall();
                    boolean cliff = onGround && isCliffAhead(platforms, SENSOR_LOOKAHEAD);
                    if (wall || cliff || enrageTimer >= ENRAGE_DURATION) {
                        velocity.x = 0;
                        changeState(EnemyState.EVADE); // brief recovery beat
                    }
                } else {
                    // ── (b) Walk home ──
                    walkHome(platforms);
                }
                break;

            case EVADE:
                // Short stylistic dodge/recovery, then head home.
                velocity.x = 0;
                if (anim.isFinished(EnemyState.EVADE)) {
                    // Face toward home; flip via TURN if needed, then WALK home.
                    boolean homeOnRight = homeX > bounds.x;
                    // Mark enrage as spent so WALK enters its "walk home" branch.
                    enrageTimer = ENRAGE_DURATION;
                    if (homeOnRight != facingRight) {
                        beginTurn(EnemyState.WALK);
                    } else {
                        changeState(EnemyState.WALK);
                    }
                }
                break;

            case TURN:
                velocity.x = 0;
                if (anim.isFinished(EnemyState.TURN)) {
                    faceRight(!facingRight);
                    changeState(postTurnState);
                }
                break;

            default:
                break;
        }
    }

    /** Walk toward the home X; on arrival, reset facing and resume IDLE watch. */
    private void walkHome(Array<Rectangle> platforms) {
        float dx = homeX - bounds.x;
        if (Math.abs(dx) <= HOME_SLACK) {
            // Arrived. Snap to home, restore original facing, watch again.
            bounds.x = homeX;
            velocity.x = 0;
            if (facingRight != homeFacingRight) {
                beginTurn(EnemyState.IDLE);
            } else {
                changeState(EnemyState.IDLE);
            }
            return;
        }

        boolean goRight = dx > 0;
        if (goRight != facingRight) {
            // Need to turn to walk the other way.
            beginTurn(EnemyState.WALK);
            return;
        }
        velocity.x = goRight ? HOME_SPEED : -HOME_SPEED;

        // Don't walk off the world chasing home.
        if (isWallAhead(platforms, SENSOR_LOOKAHEAD) || (onGround && isCliffAhead(platforms, SENSOR_LOOKAHEAD))) {
            velocity.x = 0;
            changeState(EnemyState.IDLE);
        }
    }

    /** Enter TURN, remembering which state to resume once the flip completes. */
    private void beginTurn(EnemyState after) {
        this.postTurnState = after;
        changeState(EnemyState.TURN);
    }

    private boolean playerIsOnRight(Rectangle target) {
        if (target == null) return facingRight;
        return target.x + target.width / 2f > bounds.x + bounds.width / 2f;
    }

    /**
     * LASER STUB — intentionally does nothing for now.
     */
    @SuppressWarnings("unused")
    private void fireLaser(Rectangle target) {
        // TODO(laser): charge gun circle -> fire long beam from enemies/Laser.
    }

    @Override
    protected float getContactDamage() {
        return CONTACT_DAMAGE;
    }

    @Override
    protected String enemyTypeName() {
        return "crystalguardian";
    }

    @Override
    public void renderDebug(ShapeRenderer shapes) {
        super.renderDebug(shapes); // red hitbox
        if (!isDying()) {
            // Vision box yellow; tint the box orange while enraged for clarity.
            boolean enraged = state == EnemyState.WALK && enrageTimer < ENRAGE_DURATION;
            shapes.setColor(enraged ? Color.ORANGE : Color.YELLOW);
            shapes.rect(visionBox.x, visionBox.y, visionBox.width, visionBox.height);

            // Home marker (green tick on the floor) so you can see it return.
            shapes.setColor(Color.GREEN);
            shapes.line(homeX, bounds.y - 6, homeX, bounds.y + 6);
        }
    }

    public Rectangle getVisionBox() {
        return visionBox;
    }
}
