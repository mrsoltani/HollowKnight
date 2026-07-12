package com.Graphic.models.boss;

import com.Graphic.managers.EventBus;
import com.Graphic.models.SolidBlock;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

import java.util.EnumMap;
import java.util.Map;

public class FalseKnight {


    private static final int   MAX_HP      = 300;
    private static final float STUN_AT_PCT = 0.5f;


    private static final float WALK_SPEED    = 500f;
    private static final float CHARGE_SPEED  = 1000f;

    private static final float GRAVITY       = -2600f;
    private static final float JUMP_VELOCITY = 1000f;


    private static final float PREDICTION_TIME = 0.35f;
    private static final float LEAP_DISTANCE_SCALE = 0.7f;


    private static final float DEFENSIVE_LEAP_DISTANCE = 250f;










    private static final float FPS_IDLE            = 1f / 8f;
    private static final float FPS_RUN              = 1f / 12f;
    private static final float FPS_ATTACK_ANTIC     = 1f / 14f;
    private static final float FPS_ATTACK_SWING     = 1f / 14f;
    private static final float FPS_ATTACK_RECOVER   = 1f / 14f;
    private static final float FPS_JUMP             = 1f / 12f;
    private static final float FPS_LAND              = 1f / 10f;
    private static final float FPS_JUMP_ATTACK      = 1f / 14f;
    private static final float FPS_STUN              = 1f / 10f;
    private static final float FPS_DEATH             = 1f / 10f;






    private static final float BODY_W = 160f;
    private static final float BODY_H = 220f;







    private static final float BODY_HITBOX_Y_OFFSET = 0f;









    private static final float SPRITE_DRAW_Y_OFFSET = -50f;








    private static final float MACE_W        = 200f;
    private static final float MACE_H        = 200f;
    private static final float MACE_OFF_X    = -250f;
    private static final float MACE_OFF_Y    = -300f;
    private static final float MACE_START    = 0.00f;
    private static final float MACE_END      = 0.5f;




    private static final float JUMP_MACE_W   = 170f;
    private static final float JUMP_MACE_H   = 160f;
    private static final float JUMP_MACE_OFF_X = -190f;
    private static final float JUMP_MACE_OFF_Y = -360f;
    private static final float JUMP_MACE_START = 0.42f;
    private static final float JUMP_MACE_END   = 0.54f;

    private static final float CHARGE_W      = 230f;
    private static final float CHARGE_H      = 420f;
    private static final float CHARGE_OFF_X  = -30f;
    private static final float CHARGE_OFF_Y  = -10f;
    private boolean hasLandedOnce = false;
    private final Array<Shockwave> shockwaves = new Array<>();


    public enum State {
        IDLE, WALK, TURN,
        ATTACK_ANTIC, ATTACK, ATTACK_RECOVER,
        JUMP_ANTIC, AIRBORNE, LAND,
        JUMP_ATTACK_ANTIC, JUMP_ATTACK, JUMP_ATTACK_RECOVER,
        STUN, STUN_RECOVER,
        DEAD
    }















    private State   state     = State.IDLE;
    private float   stateTime = 0f;
    private boolean facingRight = true;
    private boolean isPhase2    = false;
    private boolean stunTriggered = false;

    private float x, y;
    private float velY = 0f;
    private boolean onGround = false;

    private int hp = MAX_HP;
    private float leapTargetX = 0f;

    private Array<SolidBlock> solidBlocks = new Array<>();
    private SolidBlock currentGroundBlock = null;







    private Rectangle roomBounds = null;

    public void setSolidBlocks(Array<SolidBlock> solidBlocks) {
        this.solidBlocks = (solidBlocks != null) ? solidBlocks : new Array<>();
    }


    public void setRoomBounds(Rectangle roomBounds) {
        this.roomBounds = roomBounds;
    }

    private float prevPlayerCX = 0f;
    private float playerVelX   = 0f;
    private boolean hasTrackedPlayerOnce = false;

    private final FalseKnightAI ai = new FalseKnightAI();

    private final Map<State, Animation<TextureRegion>> animations = new EnumMap<>(State.class);
    private final Map<State, Texture>                  textures   = new EnumMap<>(State.class);
    private TextureRegion currentFrame;

    private final Rectangle bodyBox   = new Rectangle();
    private final Rectangle weaponBox = new Rectangle();

    private float spriteW = 400f;
    private float spriteH = 400f;

    public FalseKnight(float spawnX, float spawnY) {
        this.x = spawnX;
        this.y = spawnY;
        loadAnimations();
        updateFrame();
    }

    private void loadAnimations() {
        load("Idle.png",           5,  FPS_IDLE,          true,  State.IDLE);
        load("Run.png",            5,  FPS_RUN,           true,  State.WALK);
        load("Turn.png",           2,  1f/10f,            false, State.TURN);
        load("Attack Antic.png",   6,  FPS_ATTACK_ANTIC,  false, State.ATTACK_ANTIC);
        load("Attack.png",         3,  FPS_ATTACK_SWING,  false, State.ATTACK);
        load("Attack Recover.png", 5,  FPS_ATTACK_RECOVER,false, State.ATTACK_RECOVER);
        load("Jump Antic.png",     3,  FPS_JUMP,          false, State.JUMP_ANTIC);
        load("Jump.png",           4,  FPS_JUMP,          false, State.AIRBORNE);
        load("Land.png",           5,  FPS_LAND,          false, State.LAND);
        load("Jump Attack.png",    8,  FPS_JUMP_ATTACK,   false, State.JUMP_ATTACK);
        load("Stun Recover.png",   6,  FPS_STUN,          false, State.STUN_RECOVER);
        load("Body.png",           5,  FPS_STUN,          true,  State.STUN);
        load("DeathHit.png",       3,  FPS_DEATH,         false, State.DEAD);
    }

    private void load(String file, int frames, float fps, boolean loop, State s) {
        Texture tex = new Texture(Gdx.files.internal("sprites/boss/" + file));
        textures.put(s, tex);

        if (s == State.IDLE) { spriteW = tex.getWidth() / frames; spriteH = tex.getHeight(); }

        TextureRegion[] regions = new TextureRegion[frames];
        int fw = tex.getWidth() / frames;
        for (int i = 0; i < frames; i++)
            regions[i] = new TextureRegion(tex, i * fw, 0, fw, tex.getHeight());

        Animation<TextureRegion> anim = new Animation<>(fps, regions);
        anim.setPlayMode(loop ? Animation.PlayMode.LOOP : Animation.PlayMode.NORMAL);
        animations.put(s, anim);
    }

    public void update(float delta, Rectangle playerBounds) {
        stateTime += delta;
        trackPlayerVelocity(delta, playerBounds);

        if (!stunTriggered && hp <= MAX_HP * STUN_AT_PCT) {
            stunTriggered = true;
            enterState(State.STUN);
            EventBus.emit(EventBus.Event.FALSE_KNIGHT_STUN_ENTER);
            return;
        }

        switch (state) {
            case IDLE:
                facePlayer(playerBounds);
                FalseKnightAI.BossMove move = ai.update(delta, getBounds(), playerBounds, isPhase2, playerVelX);
                executeMoveDecision(move, playerBounds);
                break;

            case WALK: {
                facePlayer(playerBounds);
                float targetX = playerBounds.x + playerBounds.width / 2f;
                float myX     = x + spriteW / 2f;
                float dir     = targetX > myX ? 1f : -1f;
                x += dir * WALK_SPEED * delta;
                x  = clampToRoom(clampToGroundPlatform(x));
                if (Math.abs(targetX - myX) < 180f) enterState(State.ATTACK_ANTIC);
                break;
            }

            case TURN:
                if (isFinished()) enterState(State.IDLE);
                break;

            case ATTACK_ANTIC:
                if (isFinished()) enterState(State.ATTACK);
                break;

            case ATTACK:
                if (isFinished()) enterState(State.ATTACK_RECOVER);
                break;

            case ATTACK_RECOVER:
                if (isFinished()) enterState(State.IDLE);
                break;

            case JUMP_ANTIC:
                if (isFinished()) {
                    velY = JUMP_VELOCITY;
                    onGround = false;
                    enterState(State.AIRBORNE);
                    EventBus.emit(EventBus.Event.FALSE_KNIGHT_JUMP_TAKEOFF);
                }
                break;

            case AIRBORNE:
                x += (leapTargetX - (x + spriteW / 2f)) * 2.5f * delta;
                x  = clampToRoom(clampToGroundPlatform(x));
                break;

            case LAND:
                if (isFinished()) enterState(State.IDLE);
                break;

            case JUMP_ATTACK:
                x += (leapTargetX - (x + spriteW / 2f)) * 2.5f * delta;
                x  = clampToRoom(clampToGroundPlatform(x));
                break;

            case STUN:
                if (stateTime > 3.0f) {
                    isPhase2 = true;
                    ai.reset();
                    enterState(State.STUN_RECOVER);
                    EventBus.emit(EventBus.Event.FALSE_KNIGHT_RAGE);
                }
                break;

            case STUN_RECOVER:
                if (isFinished()) {
                    enterState(State.IDLE);
                    EventBus.emit(EventBus.Event.FALSE_KNIGHT_STUN_RECOVER);
                }
                break;

            case DEAD:
                break;
        }

        resolvePhysics(delta);
        updateShockwaves(delta);

        updateFrame();
    }

    private void resolvePhysics(float delta) {
        float prevY = y;

        velY += GRAVITY * delta;
        y    += velY * delta;

        float bodyLeft  = x + spriteW / 2f - BODY_W / 2f;
        float bodyRight = bodyLeft + BODY_W;

        SolidBlock landedOn = null;
        float landedTop = Float.NEGATIVE_INFINITY;

        for (SolidBlock b : solidBlocks) {
            if (b == null || b.bounds == null) continue;
            Rectangle r = b.bounds;

            boolean horizontallyOverlaps = bodyRight > r.x && bodyLeft < r.x + r.width;
            if (!horizontallyOverlaps) continue;

            float top = r.y + r.height;
            if (top <= prevY + 0.5f && y <= top && top > landedTop) {
                landedTop = top;
                landedOn  = b;
            }
        }

        if (landedOn != null) {
            y = landedTop;
            velY = 0f;

            boolean justLanded = !onGround;
            onGround = true;
            currentGroundBlock = landedOn;

            if (justLanded) {
                if (!hasLandedOnce) {
                    hasLandedOnce = true;
                    EventBus.emit(EventBus.Event.FALSE_KNIGHT_LAND_FIRST);
                }
                if (state == State.AIRBORNE) {
                    enterState(State.LAND);
                    EventBus.emit(EventBus.Event.FALSE_KNIGHT_JUMP_LAND);
                } else if (state == State.JUMP_ATTACK) {
                    enterState(State.ATTACK_RECOVER);
                    EventBus.emit(EventBus.Event.FALSE_KNIGHT_SLAM_IMPACT);
                    spawnShockwaves();
                }

            }
        } else {
            onGround = false;
        }
    }

    private float clampToGroundPlatform(float targetX) {
        if (currentGroundBlock == null) return targetX;
        float left  = currentGroundBlock.bounds.x;
        float right = currentGroundBlock.bounds.x + currentGroundBlock.bounds.width - spriteW;
        if (right < left) return targetX;
        return MathUtils.clamp(targetX, left, right);
    }

    private float clampCenterToGroundPlatform(float centerX) {
        if (currentGroundBlock == null) return centerX;
        float left  = currentGroundBlock.bounds.x;
        float right = currentGroundBlock.bounds.x + currentGroundBlock.bounds.width;
        if (right < left) return centerX;
        return MathUtils.clamp(centerX, left, right);
    }


    private float clampToRoom(float targetX) {
        if (roomBounds == null) return targetX;
        float left  = roomBounds.x;
        float right = roomBounds.x + roomBounds.width - spriteW;
        if (right < left) return targetX;
        return MathUtils.clamp(targetX, left, right);
    }


    private float clampCenterToRoom(float centerX) {
        if (roomBounds == null) return centerX;
        float left  = roomBounds.x;
        float right = roomBounds.x + roomBounds.width;
        if (right < left) return centerX;
        return MathUtils.clamp(centerX, left, right);
    }

    private void trackPlayerVelocity(float delta, Rectangle playerBounds) {
        float currentPlayerCX = playerBounds.x + playerBounds.width / 2f;

        if (!hasTrackedPlayerOnce) {
            prevPlayerCX = currentPlayerCX;
            hasTrackedPlayerOnce = true;
            return;
        }

        if (delta > 0f) {
            float instantVel = (currentPlayerCX - prevPlayerCX) / delta;
            playerVelX = MathUtils.lerp(playerVelX, instantVel, 0.35f);
        }
        prevPlayerCX = currentPlayerCX;
    }


    private float predictPlayerCenterX(Rectangle playerBounds) {
        float currentPlayerCX = playerBounds.x + playerBounds.width / 2f;
        float predicted = currentPlayerCX + playerVelX * PREDICTION_TIME;
        return clampCenterToRoom(clampCenterToGroundPlatform(predicted));
    }

    private void executeMoveDecision(FalseKnightAI.BossMove move, Rectangle playerBounds) {
        if (move == FalseKnightAI.BossMove.IDLE) return;

        float playerCX          = playerBounds.x + playerBounds.width / 2f;
        float predictedPlayerCX = predictPlayerCenterX(playerBounds);
        float myCX               = x + spriteW / 2f;

        switch (move) {
            case SLAM:
                facePlayer(playerBounds);
                enterState(State.ATTACK_ANTIC);
                EventBus.emit(EventBus.Event.FALSE_KNIGHT_ATTACK_WINDUP);
                break;

            case CHARGE:
                facePlayer(playerBounds);
                leapTargetX = predictedPlayerCX;
                enterState(State.WALK);
                EventBus.emit(EventBus.Event.FALSE_KNIGHT_CHARGE_SWING);
                break;

            case OFFENSIVE_LEAP:
                facePlayer(playerBounds);
                leapTargetX = clampCenterToRoom(clampCenterToGroundPlatform(
                    myCX + (predictedPlayerCX - myCX) * LEAP_DISTANCE_SCALE));
                velY = JUMP_VELOCITY;
                onGround = false;
                enterState(State.JUMP_ANTIC);
                break;

            case DEFENSIVE_LEAP: {
                float backDir = (playerCX > myCX) ? -1f : 1f;
                leapTargetX = clampCenterToRoom(clampCenterToGroundPlatform(
                    myCX + backDir * DEFENSIVE_LEAP_DISTANCE));
                velY = JUMP_VELOCITY * 0.8f;
                onGround = false;
                enterState(State.JUMP_ANTIC);
                break;
            }

            case POWER_SLAM:
                facePlayer(playerBounds);
                leapTargetX = clampCenterToRoom(clampCenterToGroundPlatform(
                    myCX + (predictedPlayerCX - myCX) * LEAP_DISTANCE_SCALE));
                velY = JUMP_VELOCITY * 1.2f;
                onGround = false;
                enterState(State.JUMP_ATTACK);
                EventBus.emit(EventBus.Event.FALSE_KNIGHT_JUMP_TAKEOFF);
                break;
        }
    }

    public void takeDamage(int amount) {
        if (state == State.DEAD || state == State.STUN) return;
        hp = Math.max(0, hp - amount);
        EventBus.emit(isPhase2 ? EventBus.Event.FALSE_KNIGHT_HIT_PHASE2 : EventBus.Event.FALSE_KNIGHT_HIT);
        if (hp <= 0) {
            enterState(State.DEAD);
            EventBus.emit(EventBus.Event.FALSE_KNIGHT_DEATH);
        }
    }
    private void spawnShockwaves() {
        float centerX = x + spriteW / 2f;
        shockwaves.add(new Shockwave(centerX, y, true));
        shockwaves.add(new Shockwave(centerX, y, false));
    }

    public boolean isDead()     { return state == State.DEAD; }
    public boolean isStunned()  { return state == State.STUN || state == State.STUN_RECOVER; }
    public int     getHp()      { return hp; }
    public int     getMaxHp()   { return MAX_HP; }

    public Rectangle getBodyBox() {
        float bodyLeft = x + spriteW / 2f - BODY_W / 2f;
        bodyBox.set(bodyLeft, y + BODY_HITBOX_Y_OFFSET, BODY_W, BODY_H);
        return bodyBox;
    }

    public Rectangle getWeaponBox() {
        float cx  = x + spriteW / 2f;
        float cy  = y + spriteH / 2f + SPRITE_DRAW_Y_OFFSET;
        float dir = facingRight ? -1f : 1f;

        switch (state) {
            case ATTACK:
                if (stateTime < MACE_START || stateTime > MACE_END) return null;
                weaponBox.set(cx + dir * MACE_OFF_X - MACE_W / 2f, cy + MACE_OFF_Y, MACE_W, MACE_H);
                return weaponBox;
            case JUMP_ATTACK:
                if (stateTime < JUMP_MACE_START || stateTime > JUMP_MACE_END) return null;
                weaponBox.set(cx + dir * JUMP_MACE_OFF_X - JUMP_MACE_W / 2f, cy + JUMP_MACE_OFF_Y, JUMP_MACE_W, JUMP_MACE_H);
                return weaponBox;
            case WALK:
                weaponBox.set(cx + dir * CHARGE_OFF_X - CHARGE_W / 2f, cy + CHARGE_OFF_Y - CHARGE_H / 2f, CHARGE_W, CHARGE_H);
                return weaponBox;
            default:
                return null;
        }
    }

    public Rectangle getVulnerableBox() {
        if (state == State.STUN || state == State.STUN_RECOVER) {
            Rectangle v = new Rectangle();
            v.set(x + spriteW / 2f - 50f, y + 20f, 100f, 80f);
            return v;
        }
        return getBodyBox();
    }

    public Rectangle getBounds() { return getBodyBox(); }

    public void render(SpriteBatch batch) {
        if (currentFrame == null) return;
        float phase2Scale = isPhase2 ? 1.05f : 1f;
        float w = spriteW * phase2Scale;
        float h = spriteH * phase2Scale;
        float drawY = y + SPRITE_DRAW_Y_OFFSET;

        if (!facingRight) batch.draw(currentFrame, x, drawY, w, h);
        else               batch.draw(currentFrame, x + w, drawY, -w, h);
        for (Shockwave sw : shockwaves) sw.render(batch);

    }

    public void drawHitboxes(ShapeRenderer sr) {
        sr.setColor(com.badlogic.gdx.graphics.Color.RED);
        Rectangle b = getBodyBox();
        sr.rect(b.x, b.y, b.width, b.height);

        Rectangle w = getWeaponBox();
        if (w != null) {
            sr.setColor(com.badlogic.gdx.graphics.Color.ORANGE);
            sr.rect(w.x, w.y, w.width, w.height);
        }

        if (isStunned()) {
            sr.setColor(com.badlogic.gdx.graphics.Color.YELLOW);
            Rectangle v = getVulnerableBox();
            sr.rect(v.x, v.y, v.width, v.height);
        }

        if (roomBounds != null) {
            sr.setColor(com.badlogic.gdx.graphics.Color.SKY);
            sr.rect(roomBounds.x, roomBounds.y, roomBounds.width, roomBounds.height);
        }
        sr.setColor(com.badlogic.gdx.graphics.Color.MAGENTA);
        for (Shockwave sw : shockwaves) sw.drawDebug(sr);
    }

    private void enterState(State next) {
        if (state == next) return;
        state     = next;
        stateTime = 0f;
    }

    private void facePlayer(Rectangle playerBounds) {
        float playerCX = playerBounds.x + playerBounds.width / 2f;
        float myCX     = x + spriteW / 2f;
        boolean shouldFaceRight = playerCX > myCX;
        if (shouldFaceRight != facingRight) {
            facingRight = shouldFaceRight;
        }
    }

    private boolean isFinished() {
        Animation<TextureRegion> anim = animations.get(state);
        return anim != null && anim.isAnimationFinished(stateTime);
    }

    private void updateFrame() {
        Animation<TextureRegion> anim = animations.getOrDefault(state, animations.get(State.IDLE));
        if (anim != null) currentFrame = anim.getKeyFrame(stateTime);
    }
    private void updateShockwaves(float delta) {
        for (int i = shockwaves.size - 1; i >= 0; i--) {
            Shockwave sw = shockwaves.get(i);
            sw.update(delta, roomBounds);
            if (!sw.isAlive()) shockwaves.removeIndex(i);
        }
    }
    public Array<Rectangle> getShockwaveHitboxes() {
        Array<Rectangle> boxes = new Array<>();
        for (Shockwave sw : shockwaves) boxes.add(sw.getHitbox());
        return boxes;
    }

    public void dispose() {
        textures.values().forEach(Texture::dispose);
        textures.clear();
        animations.clear();
        Shockwave.disposeShared();

    }
}

