package com.Graphic.models;

import com.Graphic.managers.CameraManager;
import com.Graphic.managers.CharmManager;
import com.Graphic.managers.EventBus;
import com.Graphic.managers.InputManager;
import com.Graphic.models.enemies.BaseEnemy;
import com.Graphic.utils.EffectSpawnData;
import com.Graphic.utils.GameAction;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

import java.util.EnumMap;
import java.util.Map;

public class Player {

    public static final int MAX_HEALTH     = 5;
    public static final int MAX_SOUL       = 99;
    public static final int SOUL_HEAL_COST = 33;

    private int health;
    private int soul;
    private boolean godMode=false;

    private boolean invincible;
    private float   invincibleTimer;
    private static final float INVINCIBLE_DURATION = 1.0f;

    private static final float GRAVITY        = -1500f;
    private static final float MAX_FALL_SPEED = -900f;
    private static final float MOVE_SPEED     = 250f;
    private static final float JUMP_VELOCITY  = 950f;

    private static final float KNOCKBACK_X    = 300f;
    private static final float KNOCKBACK_Y    = 180f;


    private static final float WALL_SLIDE_SPEED      = -150f;
    private static final float WALL_JUMP_VELOCITY_Y  = 900f;
    private static final float WALL_JUMP_VELOCITY_X  = 400f;
    private static final float WALL_PROBE_DISTANCE   = 6f;

    private static final float ATTACK_RANGE           = 140f;
    private static final float ATTACK_KNOCKBACK_FORCE = 300f;

    private float dashTimer           = 0f;
    private float dashCooldownTimer   = 0f;
    private float attackCooldownTimer = 0f;
    private float airStallTimer       = 0f;
    private float footstepTimer       = 0f;

    private float focusTimer          = 0f;
    private static final float FOCUS_DURATION = 1.0f;


    private float spellTapTimer       = 0f;
    private static final float SPELL_TAP_THRESHOLD = 0.15f;

    private static final float AIR_STALL_DURATION = 0.1f;
    private static final float POGO_BOUNCE_SPEED  = 1200f;
    private static final float POGO_MIN_SWING_TIME = 0.1f;

    private static final float HITBOX_WIDTH_RATIO  = 0.6f;
    private static final float HITBOX_HEIGHT_RATIO = 0.85f;


    private static final float WALL_JUMP_LOCK   = 0.18f;
    private static final float WALL_RECLING_COOLDOWN = 0.25f;
    private float wallJumpLockTimer     = 0f;
    private float wallReclingCooldown   = 0f;
    private boolean lastWallJumpOnRight = false;

    private float recoilLockTimer = 0f;
    private static final float RECOIL_DURATION = 0.1f;


    private static final float WALL_JUMP_GRACE_TIME = 0.15f;
    private float wallJumpGraceTimer = 0f;
    private boolean graceWallOnRight = false;

    private final Rectangle bounds      = new Rectangle();
    public final Vector2   velocity    = new Vector2();
    private final Vector2   spawnPoint;
    private final Vector2   lastSafePosition = new Vector2();

    private boolean onGround            = false;
    private float fallStartY   = 0f;
    private boolean wasFalling = false;
    private boolean facingRight         = true;
    private boolean doubleJumpAvailable = true;
    private boolean inputEnabled        = true;


    private boolean wallClingActive;
    private boolean wallOnRight;


    private Array<SolidBlock> transientBlocksRef;
    private Rectangle breakableWallBounds;
    boolean hitWall=false;

    private float deathTimer = 0f;

    private static final float DEATH_DELAY = 1.5f;




    private boolean fireballCastTriggered = false;
    private boolean screamCastTriggered   = false;

    private static final float FPS_IDLE         = 1f / 10f;
    private static final float FPS_RUN          = 1f / 16f;
    private static final float FPS_AIRBORNE     = 1f / 14f;
    private static final float FPS_FALL         = 1f / 12f;
    private static final float FPS_RUN_TO_IDLE  = 1f / 14f;
    private static final float FPS_ATTACK       = 1f / 18f;
    private static final float FPS_SPELL        = 1f / 15f;
    private static final float FPS_IDLE_HURT    = 1f / 16f;
    private static final float FPS_DEATH        = 1f / 10f;

    private final Map<PlayerState, Animation<TextureRegion>> animations = new EnumMap<>(PlayerState.class);

    private PlayerState state     = PlayerState.IDLE;
    private float       stateTime = 0f;

    public Player(TextureAtlas atlas, float spawnX, float spawnY) {
        this.spawnPoint = new Vector2(spawnX, spawnY);
        this.lastSafePosition.set(spawnX, spawnY);
        this.health     = 5;
        this.soul       = 99;

        loadAnimations(atlas);

        TextureRegion ref = animations.get(PlayerState.IDLE).getKeyFrame(0);
        float hitboxW = ref.getRegionWidth()  * HITBOX_WIDTH_RATIO;
        float hitboxH = ref.getRegionHeight() * HITBOX_HEIGHT_RATIO;
        bounds.setSize(hitboxW, hitboxH);
        bounds.setPosition(spawnX, spawnY);
    }

    private void loadAnimations(TextureAtlas atlas) {
        add(atlas, PlayerState.IDLE,        "Idle",        FPS_IDLE,        Animation.PlayMode.LOOP);
        add(atlas, PlayerState.RUN,         "Run",         FPS_RUN,         Animation.PlayMode.LOOP);
        add(atlas, PlayerState.RUN_TO_IDLE, "Run To Idle", FPS_RUN_TO_IDLE, Animation.PlayMode.NORMAL);
        add(atlas, PlayerState.AIRBORNE,    "Airborne",    FPS_AIRBORNE,    Animation.PlayMode.NORMAL);
        add(atlas, PlayerState.FALL,        "Fall",        FPS_FALL,        Animation.PlayMode.LOOP);
        add(atlas, PlayerState.ATTACK,      "Slash",       FPS_ATTACK,      Animation.PlayMode.NORMAL);
        add(atlas, PlayerState.ATTACK_ALT,  "SlashAlt",    FPS_ATTACK,      Animation.PlayMode.NORMAL);
        add(atlas, PlayerState.IDLE_HURT,   "Idle Hurt",   FPS_IDLE_HURT,   Animation.PlayMode.NORMAL);
        add(atlas, PlayerState.DEAD,        "Death",       FPS_DEATH,       Animation.PlayMode.NORMAL);
        add(atlas, PlayerState.DASH,        "Dash",        1f / 15f,        Animation.PlayMode.NORMAL);
        add(atlas, PlayerState.SHADOW_DASH, "Shadow Dash", 1f / 15f,        Animation.PlayMode.NORMAL);
        add(atlas, PlayerState.DOUBLE_JUMP, "Double Jump", 1f / 14f,        Animation.PlayMode.NORMAL);
        add(atlas, PlayerState.DOWN_SLASH,  "DownSlash",   FPS_ATTACK,      Animation.PlayMode.NORMAL);
        add(atlas, PlayerState.WALL_SLIDE,  "Wall Slide",  1f / 12f,        Animation.PlayMode.LOOP);
        add(atlas, PlayerState.WALL_JUMP,   "Walljump",    1f / 14f,        Animation.PlayMode.NORMAL);

        add(atlas, PlayerState.FOCUS_START, "Focus Start", 1f / 15f,        Animation.PlayMode.NORMAL);
        add(atlas, PlayerState.FOCUS,       "Focus",       1f / 12f,        Animation.PlayMode.LOOP);
        add(atlas, PlayerState.FOCUS_GET,   "Focus Get",   1f / 15f,        Animation.PlayMode.NORMAL);
        add(atlas, PlayerState.FOCUS_END,   "Focus End",   1f / 15f,        Animation.PlayMode.NORMAL);


        add(atlas, PlayerState.FIREBALL_CAST, "Fireball Cast", FPS_SPELL, Animation.PlayMode.NORMAL);
        add(atlas, PlayerState.SCREAM_CAST,   "Scream",        FPS_SPELL, Animation.PlayMode.NORMAL);
    }

    private void add(TextureAtlas atlas, PlayerState s, String regionName, float frameDuration, Animation.PlayMode mode) {
        Array<TextureAtlas.AtlasRegion> frames = atlas.findRegions(regionName);
        if (frames.size == 0) return;
        animations.put(s, new Animation<>(frameDuration, frames, mode));
    }




    public void update(float delta, Array<SolidBlock> solidBlocks, Rectangle wallBound, Array<BaseEnemy> enemies) {
        this.transientBlocksRef = solidBlocks;
        this.breakableWallBounds=wallBound;
        stateTime += delta;
        tickInvincibility(delta);
        if (isDead()) {
            velocity.set(0, 0);
            deathTimer += delta;
        }
        if (attackCooldownTimer > 0) attackCooldownTimer -= delta;
        if (dashCooldownTimer > 0)   dashCooldownTimer -= delta;
        if (airStallTimer > 0)       airStallTimer -= delta;
        if (wallJumpGraceTimer > 0) wallJumpGraceTimer -= delta;
        if (recoilLockTimer > 0)    recoilLockTimer -= delta;

        updateWallClingState();
        handleInput();
        applyGravity(delta);
        moveAndCollide(delta, solidBlocks);


        checkPogo(enemies);

        updateState(delta);
        updateFootsteps(delta);
    }
    public boolean isReadyToRespawn() {
        return isDead() && deathTimer >= DEATH_DELAY;
    }

    private void checkPogo(Array<BaseEnemy> enemies) {
        if (state != PlayerState.DOWN_SLASH) return;
        if (stateTime < POGO_MIN_SWING_TIME) return;

        Rectangle downBox = getDownwardAttackBox();


        if (enemies != null) {
            for (BaseEnemy enemy : enemies) {

                if (!enemy.isDead() && downBox.overlaps(enemy.getBounds())) {
                    triggerPogoBounce();
                    return;
                }
            }
        }


        if (transientBlocksRef != null) {
            for (SolidBlock b : transientBlocksRef) {
                if (b.isDeadly && downBox.overlaps(b.bounds)) {
                    triggerPogoBounce();
                    break;
                }
            }
        }
    }

    private void tickInvincibility(float delta) {
        if (!invincible) return;
        invincibleTimer -= delta;
        if (invincibleTimer <= 0f) invincible = false;
    }

    private void updateFootsteps(float delta) {
        if (onGround && state == PlayerState.RUN && velocity.x != 0) {
            footstepTimer -= delta;
            if (footstepTimer <= 0f) {
                EventBus.emit(EventBus.Event.PLAYER_RUN);
                footstepTimer = 0.32f;
            }
        } else {
            footstepTimer = 0f;
        }
    }

    private boolean isTouchingWall(boolean rightSide) {
        if (transientBlocksRef == null) return false;

        float inset = 6f;
        Rectangle probe = new Rectangle(
            rightSide ? bounds.x + bounds.width : bounds.x - WALL_PROBE_DISTANCE,
            bounds.y + inset,
            WALL_PROBE_DISTANCE,
            bounds.height - inset * 2f
        );

        float covered = 0f;
        for (SolidBlock b : transientBlocksRef) {
            if (b.isDeadly || !b.slide) continue;
            if (!probe.overlaps(b.bounds)) continue;

            float overlapTop = Math.min(probe.y + probe.height, b.bounds.y + b.bounds.height);
            float overlapBot = Math.max(probe.y, b.bounds.y);
            if (overlapTop > overlapBot) covered += overlapTop - overlapBot;
        }

        return covered >= probe.height * 0.70f;
    }

    private void updateWallClingState() {
        wallClingActive = false;

        if (onGround
            || state == PlayerState.DASH || state == PlayerState.SHADOW_DASH
            || state == PlayerState.DEAD || state == PlayerState.IDLE_HURT
            || state == PlayerState.FOCUS_START || state == PlayerState.FOCUS
            || state == PlayerState.FOCUS_GET   || state == PlayerState.FOCUS_END
            || state == PlayerState.FIREBALL_CAST || state == PlayerState.SCREAM_CAST) {
            if (state == PlayerState.WALL_SLIDE) setState(PlayerState.FALL);
            return;
        }

        boolean leftHeld  = Gdx.input.isKeyPressed(InputManager.getKeyCode(GameAction.LEFT));
        boolean rightHeld = Gdx.input.isKeyPressed(InputManager.getKeyCode(GameAction.RIGHT));

        if (leftHeld && rightHeld) {
            if (state == PlayerState.WALL_SLIDE) setState(PlayerState.FALL);
            return;
        }

        boolean physicalWallRight = isTouchingWall(true);
        boolean physicalWallLeft  = isTouchingWall(false);

        boolean touchRight = false;
        boolean touchLeft  = false;

        if (state == PlayerState.WALL_SLIDE) {
            if (wallOnRight) {
                touchRight = physicalWallRight && !leftHeld;
            } else {
                touchLeft  = physicalWallLeft && !rightHeld;
            }
        } else {
            touchRight = rightHeld && physicalWallRight;
            touchLeft  = leftHeld  && physicalWallLeft;
        }

        if (wallReclingCooldown > 0) {
            if ((touchRight && lastWallJumpOnRight) || (touchLeft && !lastWallJumpOnRight)) {
                if (state == PlayerState.WALL_SLIDE) setState(PlayerState.FALL);
                return;
            }
        }

        if (touchRight || touchLeft) {
            wallClingActive     = true;
            wallOnRight         = touchRight;
            facingRight         = wallOnRight;
            doubleJumpAvailable = true;
            wallJumpGraceTimer  = 0f;
            if (state != PlayerState.WALL_SLIDE) setState(PlayerState.WALL_SLIDE);
        } else if (state == PlayerState.WALL_SLIDE) {
            wallJumpGraceTimer = WALL_JUMP_GRACE_TIME;
            graceWallOnRight   = wallOnRight;
            setState(velocity.y < 0 ? PlayerState.FALL : PlayerState.AIRBORNE);
        }
    }
    private void handleInput() {

        if (!inputEnabled
            || state == PlayerState.DEAD
            || state == PlayerState.IDLE_HURT
            || state == PlayerState.DASH
            || state == PlayerState.SHADOW_DASH
            || state == PlayerState.FIREBALL_CAST
            || state == PlayerState.SCREAM_CAST) return;

        if (wallJumpLockTimer   > 0) wallJumpLockTimer   -= Gdx.graphics.getDeltaTime();
        if (wallReclingCooldown > 0) wallReclingCooldown -= Gdx.graphics.getDeltaTime();

        PlayerStats stats            = CharmManager.getStats();
        boolean focusJustPressed     = Gdx.input.isKeyJustPressed(InputManager.getKeyCode(GameAction.FOCUS_CAST));
        boolean focusHeld            = Gdx.input.isKeyPressed(InputManager.getKeyCode(GameAction.FOCUS_CAST));
        boolean leftHeld             = Gdx.input.isKeyPressed(InputManager.getKeyCode(GameAction.LEFT));
        boolean rightHeld            = Gdx.input.isKeyPressed(InputManager.getKeyCode(GameAction.RIGHT));
        boolean downHeld             = Gdx.input.isKeyPressed(InputManager.getKeyCode(GameAction.DOWN));
        boolean upHeld               = Gdx.input.isKeyPressed(InputManager.getKeyCode(GameAction.UP));
        boolean jumpJustPressed      = Gdx.input.isKeyJustPressed(InputManager.getKeyCode(GameAction.JUMP));


        if (focusJustPressed) {

            if (upHeld && soul >= SOUL_HEAL_COST) {
                soul -= SOUL_HEAL_COST;
                velocity.x = 0;
                setState(PlayerState.SCREAM_CAST);
                return;
            } else if (!onGround && soul >= SOUL_HEAL_COST) {
                soul -= SOUL_HEAL_COST;
                setState(PlayerState.FIREBALL_CAST);
                triggerAirStall();
                return;
            } else if (onGround) {
                spellTapTimer = 0.001f;
            }
        }

        if (focusHeld) {
            if (spellTapTimer > 0f) {
                spellTapTimer += Gdx.graphics.getDeltaTime();

                if (spellTapTimer >= SPELL_TAP_THRESHOLD) {
                    spellTapTimer = 0f;
                    if ((state == PlayerState.IDLE || state == PlayerState.RUN || state == PlayerState.RUN_TO_IDLE || state == PlayerState.FOCUS_END)
                        && soul >= SOUL_HEAL_COST && health < MAX_HEALTH) {
                        setState(PlayerState.FOCUS_START);
                    }
                }
            }
            if (state == PlayerState.FOCUS_START || state == PlayerState.FOCUS || state == PlayerState.FOCUS_GET) {
                velocity.x = 0;
                return;
            }
        } else {
            if (state == PlayerState.FOCUS_START || state == PlayerState.FOCUS) {
                setState(PlayerState.FOCUS_END);
                focusTimer = 0f;
            } else if (spellTapTimer > 0f && spellTapTimer < SPELL_TAP_THRESHOLD) {

                if (soul >= SOUL_HEAL_COST) {
                    soul -= SOUL_HEAL_COST;
                    setState(PlayerState.FIREBALL_CAST);
                    velocity.x = 0;
                }
            }
            spellTapTimer = 0f;
        }


        if (wallJumpLockTimer <= 0 && recoilLockTimer <= 0) {
            if (rightHeld) {
                velocity.x  = MOVE_SPEED;
                facingRight = true;
                if (state == PlayerState.IDLE || state == PlayerState.RUN_TO_IDLE
                    || state == PlayerState.FOCUS_END || state == PlayerState.FOCUS_GET)
                    setState(PlayerState.RUN);
            } else if (leftHeld) {
                velocity.x  = -MOVE_SPEED;
                facingRight = false;
                if (state == PlayerState.IDLE || state == PlayerState.RUN_TO_IDLE
                    || state == PlayerState.FOCUS_END || state == PlayerState.FOCUS_GET)
                    setState(PlayerState.RUN);
            } else {
                velocity.x = 0;
                if (state == PlayerState.RUN) setState(PlayerState.RUN_TO_IDLE);
            }
        }

        if (jumpJustPressed) {
            boolean canWallJump = wallClingActive || wallJumpGraceTimer > 0;

            if (canWallJump) {
                boolean effectiveWallOnRight = wallClingActive ? wallOnRight : graceWallOnRight;
                boolean pressingTowardWall = (effectiveWallOnRight && rightHeld) || (!effectiveWallOnRight && leftHeld);
                boolean pressingAwayWall   = (effectiveWallOnRight && leftHeld)  || (!effectiveWallOnRight && rightHeld);

                if (pressingTowardWall || (!pressingAwayWall)) {
                    velocity.y          = WALL_JUMP_VELOCITY_Y;
                    velocity.x          = effectiveWallOnRight ? -WALL_JUMP_VELOCITY_X : WALL_JUMP_VELOCITY_X;
                    facingRight         = !effectiveWallOnRight;
                    wallJumpLockTimer   = WALL_JUMP_LOCK;
                    lastWallJumpOnRight = effectiveWallOnRight;
                    wallReclingCooldown = WALL_RECLING_COOLDOWN;

                    wallClingActive     = false;
                    wallJumpGraceTimer  = 0f;
                    setState(PlayerState.WALL_JUMP);
                } else {
                    velocity.y  = JUMP_VELOCITY;
                    velocity.x  = pressingAwayWall
                        ? (effectiveWallOnRight ? -MOVE_SPEED : MOVE_SPEED)
                        : 0f;
                    facingRight = !effectiveWallOnRight;
                    onGround    = false;

                    wallClingActive    = false;
                    wallJumpGraceTimer = 0f;
                    setState(PlayerState.AIRBORNE);
                }
                EventBus.emit(EventBus.Event.PLAYER_JUMP, new EffectSpawnData(bounds.x + bounds.width / 2f, bounds.y, facingRight));

            } else if (onGround) {
                velocity.y = JUMP_VELOCITY;
                onGround   = false;
                setState(PlayerState.AIRBORNE);

            } else if (doubleJumpAvailable) {
                velocity.y          = JUMP_VELOCITY;
                doubleJumpAvailable = false;
                airStallTimer       = 0f;
                setState(PlayerState.DOUBLE_JUMP);
            }
        }

        if (attackCooldownTimer <= 0 && Gdx.input.isKeyJustPressed(InputManager.getKeyCode(GameAction.ATTACK))) {
            if (downHeld && !onGround) {
                setState(PlayerState.DOWN_SLASH);
                triggerAirStall();
            } else {
                if (onGround) velocity.x = 0;
                else          triggerAirStall();

                setState(MathUtils.randomBoolean() ? PlayerState.ATTACK : PlayerState.ATTACK_ALT);
            }
            attackCooldownTimer = stats.attackCooldown;
        }

        if (dashCooldownTimer <= 0
            && Gdx.input.isKeyJustPressed(InputManager.getKeyCode(GameAction.DASH))) {
            setState(stats.sharpShadowActive ? PlayerState.SHADOW_DASH : PlayerState.DASH);
            dashTimer  = stats.dashDuration;
            velocity.x = facingRight ? stats.dashSpeed : -stats.dashSpeed;
            velocity.y = 0;
            airStallTimer = 0f;
        }
    }

    private void triggerAirStall() {
        airStallTimer = AIR_STALL_DURATION;
        if (velocity.y < 0) velocity.y = 0f;
    }

    private void applyGravity(float delta) {


        if (state == PlayerState.DEAD
            || state == PlayerState.DASH
            || state == PlayerState.SHADOW_DASH
            || state == PlayerState.FIREBALL_CAST
            || state == PlayerState.SCREAM_CAST) return;

        if (airStallTimer > 0) return;

        float gravityMultiplier = 1.0f;
        boolean jumpHeld = Gdx.input.isKeyPressed(InputManager.getKeyCode(GameAction.JUMP));

        if (velocity.y > 0 && !jumpHeld)     gravityMultiplier = 2.5f;
        else if (velocity.y < 0)             gravityMultiplier = 1.2f;

        velocity.y += (GRAVITY * gravityMultiplier) * delta;

        float minSpeed = (state == PlayerState.WALL_SLIDE) ? WALL_SLIDE_SPEED : MAX_FALL_SPEED;
        if (velocity.y < minSpeed) velocity.y = minSpeed;
    }

    private void moveAndCollide(float delta, Array<SolidBlock> solidBlocks) {
        bounds.x += velocity.x * delta;
        for (SolidBlock b : solidBlocks) {
            if (b.isDeadly || !bounds.overlaps(b.bounds)) continue;
            bounds.x = velocity.x > 0 ? b.bounds.x - bounds.width : b.bounds.x + b.bounds.width;
            velocity.x = 0;
        }

        bounds.y += velocity.y * delta;
        onGround = false;

        for (SolidBlock b : solidBlocks) {
            if (b.isDeadly || !bounds.overlaps(b.bounds)) continue;
            if (velocity.y < 0) {
                bounds.y = b.bounds.y + b.bounds.height;
                onGround = true;
                doubleJumpAvailable = true;


                lastSafePosition.set(bounds.x, bounds.y);

                if (wasFalling) {
                    float fallDistance = fallStartY - bounds.y;
                    if      (fallDistance > 600f) EventBus.emit(EventBus.Event.PLAYER_LAND_HARD);
                    else if (fallDistance > 50f)  EventBus.emit(EventBus.Event.PLAYER_LAND_SOFT);
                    wasFalling = false;
                }
            } else {
                bounds.y = b.bounds.y - bounds.height;
            }
            velocity.y = 0;
        }


        if (state != PlayerState.DEAD) {
            for (SolidBlock b : solidBlocks) {
                if (b.isDeadly && bounds.overlaps(b.bounds)) {
                    handleDeadlyBlockRespawn();
                    break;
                }
            }
        }
    }

    private void handleDeadlyBlockRespawn() {
        takeDamage(1,false);

        if (health >= 0) {
            bounds.setPosition(lastSafePosition.x, lastSafePosition.y);
            invincible      = true;
            invincibleTimer = INVINCIBLE_DURATION;
            setState(PlayerState.IDLE_HURT);
            EventBus.emit(EventBus.Event.PLAYER_DAMAGED);
        }
    }

    private void updateState(float delta) {
        switch (state) {
            case IDLE:
            case RUN:
            case RUN_TO_IDLE:
                if (!onGround) {
                    setState(velocity.y < 0 ? PlayerState.FALL : PlayerState.AIRBORNE);
                } else if (state == PlayerState.RUN_TO_IDLE && isFinished()) {
                    setState(PlayerState.IDLE);
                }
                break;
            case AIRBORNE:
            case DOUBLE_JUMP:
                if (onGround) setState(velocity.x != 0 ? PlayerState.RUN : PlayerState.IDLE);
                else if (velocity.y < 0 && airStallTimer <= 0) {
                    fallStartY = bounds.y;
                    wasFalling = true;
                    setState(PlayerState.FALL);
                }
                break;
            case FALL:
                if (onGround) setState(velocity.x != 0 ? PlayerState.RUN : PlayerState.IDLE);
                break;
            case ATTACK:
            case ATTACK_ALT:
            case DOWN_SLASH:
                if (isFinished()) {
                    if (!onGround) setState(velocity.y < 0 ? PlayerState.FALL : PlayerState.AIRBORNE);
                    else           setState(velocity.x != 0 ? PlayerState.RUN : PlayerState.IDLE);
                }
                break;

            case FIREBALL_CAST:
            case SCREAM_CAST:

                velocity.set(0, 0);

                if (isFinished()) {
                    if (!onGround) setState(PlayerState.FALL);
                    else           setState(PlayerState.IDLE);
                }
                break;
            case IDLE_HURT:
                if (isFinished()) setState(PlayerState.IDLE);
                break;
            case DASH:
            case SHADOW_DASH:
                dashTimer -= delta;
                if (dashTimer <= 0) {
                    dashCooldownTimer = CharmManager.getStats().dashCooldown;
                    boolean rightHeld = Gdx.input.isKeyPressed(InputManager.getKeyCode(GameAction.RIGHT));
                    boolean leftHeld  = Gdx.input.isKeyPressed(InputManager.getKeyCode(GameAction.LEFT));
                    if (onGround) {
                        if (rightHeld) { velocity.x = MOVE_SPEED; facingRight = true; setState(PlayerState.RUN); }
                        else if (leftHeld) { velocity.x = -MOVE_SPEED; facingRight = false; setState(PlayerState.RUN); }
                        else { velocity.x = 0; setState(PlayerState.RUN_TO_IDLE); }
                    } else {
                        velocity.x = rightHeld ? MOVE_SPEED : (leftHeld ? -MOVE_SPEED : 0);
                        setState(velocity.y < 0 ? PlayerState.FALL : PlayerState.AIRBORNE);
                    }
                }
                break;
            case WALL_SLIDE:
                break;
            case WALL_JUMP:
                if (onGround) {
                    setState(velocity.x != 0 ? PlayerState.RUN : PlayerState.IDLE);
                } else if (isFinished()) {
                    setState(velocity.y >= 0 ? PlayerState.AIRBORNE : PlayerState.FALL);
                }
                break;
            case FOCUS_START:
                if (isFinished()) setState(PlayerState.FOCUS);
                break;
            case FOCUS:
                focusTimer += delta;
                if (focusTimer >= FOCUS_DURATION) {
                    if (heal()) {
                        setState(PlayerState.FOCUS_GET);
                        EventBus.emit(EventBus.Event.PLAYER_HEAL, new EffectSpawnData(bounds.x + bounds.width / 2f, bounds.y, facingRight));
                    } else {
                        setState(PlayerState.FOCUS_END);
                    }
                    focusTimer = 0f;
                }
                break;
            case FOCUS_GET:
            case FOCUS_END:
                if (isFinished()) setState(PlayerState.IDLE);
                break;
            case DEAD:
                break;
        }
    }

    public void triggerPogoBounce() {
        velocity.y = POGO_BOUNCE_SPEED;
        onGround = false;
        doubleJumpAvailable = true;
        dashCooldownTimer = 0f;
        setState(PlayerState.AIRBORNE);
    }

    public void takeDamage(int amount, boolean fromRight) {
        if (invincible|| godMode || state == PlayerState.DEAD||state == PlayerState.SHADOW_DASH) return;
        CameraManager.shake(10f, 0.25f);
        health = Math.max(0, health - amount);
        invincible      = true;
        invincibleTimer = INVINCIBLE_DURATION;

        velocity.x = fromRight ? -KNOCKBACK_X : KNOCKBACK_X;
        velocity.y = KNOCKBACK_Y;
        onGround   = false;

        if (health <= 0) {
            setState(PlayerState.DEAD);
            EventBus.emit(EventBus.Event.PLAYER_DEATH);
        } else {
            setState(PlayerState.IDLE_HURT);
            EventBus.emit(EventBus.Event.PLAYER_DAMAGED);
        }
    }

    public boolean heal() {
        if (soul < SOUL_HEAL_COST || health >= MAX_HEALTH || state == PlayerState.DEAD) return false;
        soul   -= SOUL_HEAL_COST;
        health  = Math.min(MAX_HEALTH, health + 1);
        return true;
    }

    public void gainSoul() { soul = Math.min(MAX_SOUL, soul + CharmManager.getStats().soulPerHit); }
    public void cheatFillSoul() { soul = MAX_SOUL; }
    public void cheatRestoreOneMask() { health = Math.min(MAX_HEALTH, health + 1); }

    public void reset() {
        health              = MAX_HEALTH;
        soul                = 0;
        invincible          = false;
        invincibleTimer     = 0f;
        attackCooldownTimer = 0f;
        dashCooldownTimer   = 0f;
        airStallTimer       = 0f;
        focusTimer          = 0f;
        spellTapTimer       = 0f;
        doubleJumpAvailable = true;
        inputEnabled        = true;
        wasFalling = false;
        velocity.set(0, 0);
        setState(PlayerState.IDLE);
        deathTimer = 0f;
    }

    private boolean isFinished() {
        Animation<TextureRegion> anim = animations.get(state);
        return anim != null && anim.isAnimationFinished(stateTime);
    }

    public void render(SpriteBatch batch) {
        if (invincible && (int)(invincibleTimer / 0.075f) % 2 == 0) return;

        Animation<TextureRegion> anim = animations.get(state);
        if (anim == null) return;

        TextureRegion frame = anim.getKeyFrame(stateTime);
        float frameW = frame.getRegionWidth();
        float frameH = frame.getRegionHeight();

        float drawX = bounds.x - (frameW - bounds.width)  / 2f;
        float drawY = bounds.y;

        if (facingRight) batch.draw(frame, drawX + frameW, drawY, -frameW, frameH);
        else             batch.draw(frame, drawX, drawY, frameW, frameH);
    }

    private void setState(PlayerState next) {
        if (state == next) return;

        PlayerState previous = state;

        if (state == PlayerState.FOCUS || state == PlayerState.FOCUS_START) focusTimer = 0f;

        state     = next;
        stateTime = 0f;
        checkNailHitWall(breakableWallBounds,next);
        checkNailWallCollision(next);
        dispatchUnifiedGameplayEvent(previous, next);
    }

    private void checkNailWallCollision(PlayerState structuralState) {
        if (transientBlocksRef == null) return;

        Rectangle attackBox = getAttackBoxFor(structuralState);
        if (attackBox == null) return;

        boolean hitSolid  = false;
        for (SolidBlock b : transientBlocksRef) {
            if (!attackBox.overlaps(b.bounds)) continue;
            hitSolid = true;
        }

        if(!hitSolid) return;
        EventBus.emit(EventBus.Event.PLAYER_HIT_WALL);

        if (structuralState == PlayerState.ATTACK || structuralState == PlayerState.ATTACK_ALT) {
            recoilLockTimer = RECOIL_DURATION;

            float actualKnockback = ATTACK_KNOCKBACK_FORCE;
            velocity.x = facingRight ? -actualKnockback : actualKnockback;
        }
    }

    public Rectangle getForwardAttackBox() {
        float centerX = bounds.x + bounds.width / 2f;
        float x = facingRight ? centerX : centerX - ATTACK_RANGE;
        return new Rectangle(x, bounds.y, ATTACK_RANGE, bounds.height);
    }

    public Rectangle getDownwardAttackBox() {
        float centerY = bounds.y + bounds.height / 2f;
        return new Rectangle(bounds.x, centerY - ATTACK_RANGE, bounds.width, ATTACK_RANGE);
    }

    private Rectangle getAttackBoxFor(PlayerState s) {
        if (s == PlayerState.DOWN_SLASH) return getDownwardAttackBox();
        if (s == PlayerState.ATTACK || s == PlayerState.ATTACK_ALT) return getForwardAttackBox();
        return null;
    }

    private void dispatchUnifiedGameplayEvent(PlayerState previous, PlayerState next) {
        EffectSpawnData payload = new EffectSpawnData(
            bounds.x + bounds.width / 2f, bounds.y, facingRight
        );

        if (previous == PlayerState.RUN && next != PlayerState.RUN) {
            EventBus.emit(EventBus.Event.PLAYER_STOP_WALK);
        }

        boolean comingFromDash = (previous == PlayerState.DASH || previous == PlayerState.SHADOW_DASH);

        switch (next) {
            case AIRBORNE:
                if (!comingFromDash) EventBus.emit(EventBus.Event.PLAYER_JUMP, payload);
                break;
            case DOUBLE_JUMP:   EventBus.emit(EventBus.Event.PLAYER_DOUBLE_JUMP,  payload); break;
            case DASH:          EventBus.emit(EventBus.Event.PLAYER_DASH,         payload); wasFalling = false; break;
            case SHADOW_DASH:   EventBus.emit(EventBus.Event.PLAYER_SHADOW_DASH,  payload); wasFalling = false; break;
            case ATTACK:        EventBus.emit(EventBus.Event.PLAYER_ATTACK,       payload); break;
            case ATTACK_ALT:    EventBus.emit(EventBus.Event.PLAYER_ATTACK_ALT,   payload); break;
            case DOWN_SLASH:    EventBus.emit(EventBus.Event.PLAYER_DOWN_SLASH,   payload); break;



            case FIREBALL_CAST:
                fireballCastTriggered = true;
                EventBus.emit(EventBus.Event.PLAYER_FIREBALL, payload);
                break;
            case SCREAM_CAST:
                screamCastTriggered = true;
                EventBus.emit(EventBus.Event.PLAYER_SCREAM_SPELL, payload);
                break;

            default: break;
        }
    }

    public Rectangle   getBounds()    { return bounds;   }
    public int         getHealth()    { return health;   }
    public int         getSoul()      { return soul;     }
    public int         getMaxHealth() { return MAX_HEALTH; }
    public int         getMaxSoul()   { return MAX_SOUL;   }
    public PlayerState getState()     { return state;    }
    public boolean     isDead()       { return state == PlayerState.DEAD; }
    public boolean     isInvincible() { return invincible; }
    public boolean     isFacingRight(){ return facingRight; }
    public void toggleGodMode(){
        this.godMode=!this.godMode;
    }
    public boolean isShadowDashing() {
        return state == PlayerState.SHADOW_DASH;
    }
    public void checkNailHitWall(Rectangle wallBounds,PlayerState next) {
        if (wallBounds == null) return;

        Rectangle attackBox = getAttackBoxFor(next);
        if (attackBox == null) return;

        hitWall= attackBox.overlaps(wallBounds);
    }
    public boolean getHitWall(){
        if(hitWall==true){
            hitWall=false;
            return true;
        }
        return false;
    }


    public boolean consumeFireballCastTrigger() {
        if (fireballCastTriggered) {
            fireballCastTriggered = false;
            return true;
        }
        return false;
    }


    public boolean consumeScreamCastTrigger() {
        if (screamCastTriggered) {
            screamCastTriggered = false;
            return true;
        }
        return false;
    }
}
