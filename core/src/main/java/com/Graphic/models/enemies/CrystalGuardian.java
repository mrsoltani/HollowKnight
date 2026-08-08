package com.Graphic.models.enemies;

import com.Graphic.managers.EventBus;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

public class CrystalGuardian extends BaseEnemy {

    // ── Sprite frame size (the raw artwork dimensions) ───────────────────────
    private static final int FRAME_W = 285;
    private static final int FRAME_H = 189;

    // ── Hitbox size ──────────────────────────────────────────────────────────
    public static float HITBOX_WIDTH  = 100f;
    public static float HITBOX_HEIGHT = 140f;

    public static float SPRITE_OFFSET_X = 0f;
    public static float SPRITE_OFFSET_Y = -15f;

    private static final float CONTACT_DAMAGE   = 2f;
    private static final float MAX_HEALTH       = 80f;
    private static final float ENRAGE_SPEED     = 300f;
    private static final float HOME_SPEED       = 160f;
    private static final float SENSOR_LOOKAHEAD = 6f;
    private static final float HOME_SLACK       = 8f;

    private static final float ENRAGE_DURATION  = 2.0f;

    // ── Vision Box Dimensions ────────────────────────────────────────────────
    private static final float VISION_WIDTH       = 460f;
    private static final float VISION_HEIGHT      = 380f;
    private static final float AGGRO_SCALE_FACTOR = 3.0f;

    // ── Laser ─────────────────────────────────────────────────────────────────
    // Lamp center measured from the final Shoot.png frame's pixels:
    // 38px in front of the hitbox center, 89px above the hitbox bottom.
    private static final float LASER_GUN_OFFSET_X  = 38f;
    private static final float LASER_GUN_OFFSET_Y  = 89f;
    private static final float LASER_HEIGHT        = 10f;   // 50% thinner damage band
    private static final float LASER_VISUAL_HEIGHT = 28f;   // 50% thinner beam art
    private static final float LASER_MAX_LENGTH    = 4000f; // fires off-screen, "into outer space"
    private static final float LASER_ORB_SIZE        = 52f;  // slightly larger clean circular orb
    private static final float LASER_ORB_PULSE_DEPTH = 0.06f; // subtle +/-6% breathing pulse
    private static final float LASER_ORB_PULSE_SPEED = 7.5f;
    private static final float SHOOT_FRAME_TIME       = 0.10f;
    private static final int   SHOOT_HOLD_FRAME_INDEX = 3; // lock the fourth frame during the beam
    private static final int   SHOOT_FRAME_COUNT      = 7;
    // The beam art is a wispy muzzle-burst: the left half of each 228px frame is
    // fully transparent and both ends fade out, so it cannot be tiled. Instead we
    // take a thin slice from the densest part of the frame (pixel columns 160-184,
    // measured from the alpha channel) and stretch it — a uniform slice stays
    // perfectly continuous at any length.
    private static final float LASER_CORE_SLICE_START = 160f / 228f;
    private static final float LASER_CORE_SLICE_END   = 184f / 228f;
    private static final float LASER_DAMAGE_RATE   = 0.08f;
    private static final float LASER_HOLD_DURATION = 2.0f; // beam stays on for 2 seconds after windup

    private final Rectangle visionBox = new Rectangle();
    private boolean playerSpotted     = false;

    /** Original spot to return to after enraging. */
    private final float homeX;
    private final boolean homeFacingRight;

    private float enrageTimer = 0f;
    private EnemyState postTurnState = EnemyState.IDLE;

    // ── Laser state ───────────────────────────────────────────────────────────
    private TextureAtlas                        laserAtlas;
    private Animation<TextureAtlas.AtlasRegion> chargeStartAnim;
    private Animation<TextureRegion>            chargedOrbAnim;
    private Animation<TextureAtlas.AtlasRegion> beamAnim;
    private Texture                            chargedOrbTexture;
    private Texture                            shootPoseTexture;
    private TextureRegion[]                    shootPoseFrames;

    private float   laserStateTime    = 0f;
    private boolean windupDone        = false;
    private float   laserEndX         = 0f;
    private float   laserDamageTimer  = 0f;
    private float   beamStateTime     = 0f;
    private float   laserHoldTimer    = 0f;
    private boolean laserRecovery     = false;
    private float   laserRecoveryTime = 0f;
    private EnemyState postLaserState = EnemyState.WALK;

    // Passed in from GameScreen — needed for raycast and damage
    private Array<Rectangle> laserPlatforms;
    private Rectangle        laserTarget;

    public CrystalGuardian(float x, float y, boolean facingRight) {
        super(x, y, HITBOX_WIDTH, HITBOX_HEIGHT, facingRight, MAX_HEALTH, true);

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

        loadLaserAssets();
        changeState(EnemyState.IDLE);
    }

    private void loadLaserAssets() {
        laserAtlas = new TextureAtlas(Gdx.files.internal("enemies/Laser/Laser.atlas"));

        // NOTE: the atlas names are misleading. "Laser Beam Cln_beam_shot_effect"
        // frames 0-7 are the GROWING CHARGE ORB, frames 8-11 are the orb bursting
        // forward (muzzle release). The "crystalLaser" regions are thin wisps we
        // don't use. Verified against the png's pixels.
        Array<TextureAtlas.AtlasRegion> growFrames = new Array<>();
        Array<TextureAtlas.AtlasRegion> beamFrames = new Array<>();

        for (int i = 0; i <= 7; i++) {
            TextureAtlas.AtlasRegion r = laserAtlas.findRegion(String.format("Laser Beam Cln_beam_shot_effect%04d", i));
            if (r != null) growFrames.add(r);
        }
        // Beam body = a slice through the same solid-orb frames (4-7); the loop
        // over slightly different frames makes the beam shimmer.
        for (int i = 4; i <= 7; i++) {
            TextureAtlas.AtlasRegion r = laserAtlas.findRegion(String.format("Laser Beam Cln_beam_shot_effect%04d", i));
            if (r != null) beamFrames.add(r);
        }

        chargeStartAnim = growFrames.size > 0 ? new Animation<>(0.09f, growFrames, Animation.PlayMode.NORMAL) : null;
        beamAnim        = beamFrames.size > 0 ? new Animation<>(0.08f, beamFrames, Animation.PlayMode.LOOP) : null;

        // Active beam orb uses a purpose-built circular crop of source frames
        // 4-7. This removes the atlas frame's horizontal haze while preserving
        // the real animated core artwork.
        chargedOrbTexture = new Texture(Gdx.files.internal("enemies/Crystalized/ChargedOrbClean.png"));
        chargedOrbTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        TextureRegion[][] orbGrid = TextureRegion.split(chargedOrbTexture, 81, 81);
        Array<TextureRegion> orbFrames = new Array<>();
        int[] orbOrder = {0, 1, 2, 3, 2, 1};
        for (int frameIndex : orbOrder) orbFrames.add(orbGrid[0][frameIndex]);
        chargedOrbAnim = new Animation<>(0.08f, orbFrames, Animation.PlayMode.LOOP);

        // Keep a separately addressable copy of all seven shoot frames so the
        // active laser can hold frame 3, then play frames 4-7 as recovery.
        shootPoseTexture = new Texture(Gdx.files.internal("enemies/Crystalized/Shoot.png"));
        shootPoseTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        TextureRegion[][] shootGrid = TextureRegion.split(shootPoseTexture, FRAME_W, FRAME_H);
        shootPoseFrames = new TextureRegion[SHOOT_FRAME_COUNT];
        for (int i = 0; i < SHOOT_FRAME_COUNT; i++) shootPoseFrames[i] = shootGrid[0][i];
    }

    private void updateVisionBox() {
        float currentWidth = playerSpotted ? (VISION_WIDTH * AGGRO_SCALE_FACTOR) : VISION_WIDTH;
        float y = bounds.y - 15f;
        float x = facingRight ? bounds.x + bounds.width : bounds.x - currentWidth;
        visionBox.set(x, y, currentWidth, VISION_HEIGHT);
    }

    @Override
    protected void updateAI(float delta, Rectangle target, Array<Rectangle> platforms) {
        updateVisionBox();

        switch (state) {
            case IDLE:
                velocity.x = 0;

                if (target != null) {
                    boolean playerOnRight = playerIsOnRight(target);
                    if (playerOnRight != facingRight) {
                        beginTurn(EnemyState.IDLE);
                        break;
                    }

                    if (visionBox.overlaps(target)) {
                        playerSpotted    = true;
                        laserStateTime   = 0f;
                        windupDone       = false;
                        laserDamageTimer = 0f;
                        beamStateTime    = 0f;
                        laserRecovery   = false;
                        laserRecoveryTime = 0f;

                        laserHoldTimer   = LASER_HOLD_DURATION;
                        changeState(EnemyState.SHOOT);
                        EventBus.emit(EventBus.Event.ENEMY_CRYSTAL_LASER_CHARGE, enemyTypeName());
                    }
                }
                break;

            case SHOOT:
                velocity.x = 0;

                if (laserRecovery) {
                    laserRecoveryTime += delta;
                    float recoveryDuration = (SHOOT_FRAME_COUNT - SHOOT_HOLD_FRAME_INDEX - 1) * SHOOT_FRAME_TIME;
                    if (laserRecoveryTime >= recoveryDuration) {
                        if (postLaserState == EnemyState.TURN) {
                            beginTurn(EnemyState.WALK);
                        } else {
                            changeState(EnemyState.WALK);
                        }
                    }
                    break;
                }

                fireLaser(delta, target);

                // Hold one fixed shoot frame for the entire active beam. Once
                // the beam ends, stay in SHOOT and play only the later frames.
                if (windupDone) {
                    laserHoldTimer -= delta;

                    if (laserHoldTimer <= 0f) {
                        enrageTimer = 0f;
                        boolean playerOnRight = playerIsOnRight(target);
                        postLaserState = playerOnRight != facingRight ? EnemyState.TURN : EnemyState.WALK;
                        laserRecovery = true;
                        laserRecoveryTime = 0f;
                        EventBus.emit(EventBus.Event.ENEMY_CRYSTAL_LASER_STOP, this);
                    }
                }
                break;

            case WALK:
                if (enrageTimer < ENRAGE_DURATION) {
                    enrageTimer += delta;
                    velocity.x = facingRight ? ENRAGE_SPEED : -ENRAGE_SPEED;

                    boolean wall  = isWallAhead(platforms, SENSOR_LOOKAHEAD) || isBlockedByWall();
                    boolean cliff = onGround && isCliffAhead(platforms, SENSOR_LOOKAHEAD);
                    if (wall || cliff || enrageTimer >= ENRAGE_DURATION) {
                        velocity.x = 0;
                        changeState(EnemyState.EVADE);
                    }
                } else {
                    walkHome(platforms);
                }
                break;

            case EVADE:
                velocity.x = 0;
                if (anim.isFinished(EnemyState.EVADE)) {
                    boolean homeOnRight = homeX > bounds.x;
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

    private void walkHome(Array<Rectangle> platforms) {
        float dx = homeX - bounds.x;
        if (Math.abs(dx) <= HOME_SLACK) {
            bounds.x = homeX;
            velocity.x = 0;
            playerSpotted = false;

            if (facingRight != homeFacingRight) {
                beginTurn(EnemyState.IDLE);
            } else {
                changeState(EnemyState.IDLE);
            }
            return;
        }

        boolean goRight = dx > 0;
        if (goRight != facingRight) {
            beginTurn(EnemyState.WALK);
            return;
        }
        velocity.x = goRight ? HOME_SPEED : -HOME_SPEED;

        if (isWallAhead(platforms, SENSOR_LOOKAHEAD) || (onGround && isCliffAhead(platforms, SENSOR_LOOKAHEAD))) {
            velocity.x = 0;
            playerSpotted = false;
            changeState(EnemyState.IDLE);
        }
    }

    private void beginTurn(EnemyState after) {
        this.postTurnState = after;
        changeState(EnemyState.TURN);
    }

    private boolean playerIsOnRight(Rectangle target) {
        if (target == null) return facingRight;
        return target.x + target.width / 2f > bounds.x + bounds.width / 2f;
    }

    /**
     * Ticks the laser logic synchronously with the game logic delta.
     */
    private void fireLaser(float delta, Rectangle target) {
        laserTarget    = target;
        laserStateTime += delta;

        // ── Charge animation windup ──────────────────────────────────────────
        // The beam turns on once the charge orb is ready and the boss reaches
        // shoot frame 4; that exact frame is then held until laser shutoff.
        if (!windupDone) {
            boolean chargeReady = chargeStartAnim == null || chargeStartAnim.isAnimationFinished(laserStateTime);
            boolean poseReady   = anim.getStateTime() >= SHOOT_HOLD_FRAME_INDEX * SHOOT_FRAME_TIME;
            if (chargeReady && poseReady) {
                windupDone = true;
                beamStateTime = 0f;
                EventBus.emit(EventBus.Event.ENEMY_CRYSTAL_LASER_START, this);
            }
            return;
        }

        // ── Beam active — tick time ──────────────────────────────────────────
        beamStateTime += delta;

        // Beam always fires full-length, perfectly horizontal, off the screen.
        laserEndX = facingRight ? getGunBarrelX() + LASER_MAX_LENGTH
                                : getGunBarrelX() - LASER_MAX_LENGTH;

        // ── Damage tick ───────────────────────────────────────────────────────
        if (target != null) {
            laserDamageTimer -= delta;
            if (laserDamageTimer <= 0f) {
                laserDamageTimer = LASER_DAMAGE_RATE;
                if (isPlayerInBeam(target)) {
                    onLaserHitPlayer();
                }
            }
        }
    }

    public boolean isPlayerInBeam(Rectangle player) {
        if (!windupDone || laserRecovery || state != EnemyState.SHOOT || isDying()) return false;
        float gunX  = getGunBarrelX();
        float gunY  = getGunBarrelY();
        float beamTop = gunY + LASER_HEIGHT / 2f;
        float beamBot = gunY - LASER_HEIGHT / 2f;

        boolean inYBand   = player.y < beamTop && player.y + player.height > beamBot;
        boolean inXRange  = facingRight
            ? (player.x + player.width > gunX && player.x < laserEndX)
            : (player.x < gunX && player.x + player.width > laserEndX);

        return inYBand && inXRange;
    }

    /** Override this in GameScreen integration to call player.takeDamage() */
    protected void onLaserHitPlayer() {
        // Broadcast damage event or handle logic inside GameScreen
    }

    private float getGunBarrelX() {
        float cx = bounds.x + bounds.width / 2f;
        return facingRight ? cx + LASER_GUN_OFFSET_X : cx - LASER_GUN_OFFSET_X;
    }

    private float getGunBarrelY() {
        return bounds.y + LASER_GUN_OFFSET_Y;
    }

    @Override
    protected float groundStepInterval() {
        return state == EnemyState.WALK ? 0.28f : 0f;
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
        super.renderDebug(shapes);
        if (!isDying()) {
            shapes.setColor(playerSpotted ? Color.ORANGE : Color.YELLOW);
            shapes.rect(visionBox.x, visionBox.y, visionBox.width, visionBox.height);
            shapes.setColor(Color.GREEN);
            shapes.line(homeX, bounds.y - 6, homeX, bounds.y + 6);

            if (state == EnemyState.SHOOT && windupDone && !laserRecovery) {
                shapes.setColor(Color.MAGENTA);
                float gunX = getGunBarrelX();
                float gunY = getGunBarrelY();
                float bx = facingRight ? gunX : gunX - LASER_MAX_LENGTH;
                shapes.rect(bx, gunY - LASER_HEIGHT / 2f, LASER_MAX_LENGTH, LASER_HEIGHT);
            }
        }
    }

    @Override
    public void render(SpriteBatch batch) {
        // Drive SHOOT explicitly: play frames 1-4 during windup, hold frame 4
        // completely still through beam fire, then play frames 5-7 once.
        if (state == EnemyState.SHOOT && !isDying() && shootPoseFrames != null) {
            int poseIndex;
            if (laserRecovery) {
                int recoveryOffset = Math.min(
                    (int)(laserRecoveryTime / SHOOT_FRAME_TIME),
                    SHOOT_FRAME_COUNT - SHOOT_HOLD_FRAME_INDEX - 2);
                poseIndex = SHOOT_HOLD_FRAME_INDEX + 1 + recoveryOffset;
            } else if (windupDone) {
                poseIndex = SHOOT_HOLD_FRAME_INDEX;
            } else {
                poseIndex = Math.min((int)(anim.getStateTime() / SHOOT_FRAME_TIME),
                    SHOOT_HOLD_FRAME_INDEX);
            }

            TextureRegion poseFrame = shootPoseFrames[poseIndex];
            boolean shouldFlip = facingRight;
            if (poseFrame.isFlipX() != shouldFlip) poseFrame.flip(true, false);
            float poseX = bounds.x + bounds.width / 2f - frameWidth / 2f + spriteOffsetX;
            float poseY = bounds.y + spriteOffsetY;
            batch.draw(poseFrame, poseX, poseY, frameWidth, frameHeight);
        } else {
            super.render(batch);
        }

        // The moment state leaves SHOOT, all laser visuals vanish instantly
        if (state != EnemyState.SHOOT || isDying()) return;

        // Beam/orb disappear while frames 4-7 finish after laser shutoff.
        if (laserRecovery) return;

        float gunX = getGunBarrelX();
        float gunY = getGunBarrelY();

        // ── Charge/burst frame at barrel ─────────────────────────────────────
        // Select the frame now, but draw it after the beam so the orb masks the
        // lamp and beam-origin seam instead of being covered by the beam.
        TextureRegion chargeFrame = null;
        if (!windupDone && chargeStartAnim != null) {
            chargeFrame = chargeStartAnim.getKeyFrame(laserStateTime);
        } else if (windupDone && chargedOrbAnim != null) {
            // Continue animating the orb throughout the active beam instead of
            // freezing on the final charge frame.
            chargeFrame = chargedOrbAnim.getKeyFrame(beamStateTime);
        }

        // ── Beam — only after windup ──────────────────────────────────────────
        if (windupDone && beamAnim != null) {
            TextureAtlas.AtlasRegion frame = beamAnim.getKeyFrame(beamStateTime);
            float beamY = gunY - LASER_VISUAL_HEIGHT / 2f;

            float uSpan = frame.getU2() - frame.getU();
            float u1 = frame.getU() + uSpan * LASER_CORE_SLICE_START;
            float u2 = frame.getU() + uSpan * LASER_CORE_SLICE_END;

            float drawX = facingRight ? gunX : gunX - LASER_MAX_LENGTH;
            batch.draw(frame.getTexture(),
                drawX, beamY, LASER_MAX_LENGTH, LASER_VISUAL_HEIGHT,
                u1, frame.getV2(), u2, frame.getV());
        }

        if (chargeFrame != null) {
            float pulse = windupDone
                ? 1f + LASER_ORB_PULSE_DEPTH * (float)Math.sin(beamStateTime * LASER_ORB_PULSE_SPEED)
                : 1f;

            if (windupDone) {
                // Clean 81x81 circular frames contain no horizontal haze. Keep
                // the pulse centered on the lamp so the orb never drifts.
                float size = LASER_ORB_SIZE * pulse;
                batch.draw(chargeFrame, gunX - size / 2f, gunY - size / 2f, size, size);
            } else {
                // Windup still uses the original atlas art and measured visual
                // center so its established charge-up alignment is preserved.
                float scale = 0.7f;
                float w = 228f * scale;
                float h = 123f * scale;
                float anchorX = 170f / 228f;
                float anchorY = 1f - (60f / 123f);
                float drawY = gunY - h * anchorY;
                if (facingRight) {
                    batch.draw(chargeFrame, gunX - w * anchorX, drawY, w, h);
                } else {
                    float orbDrawX = gunX - w * (1f - anchorX);
                    batch.draw(chargeFrame, orbDrawX + w, drawY, -w, h);
                }
            }
        }
    }

    public void setPlatforms(Array<Rectangle> platforms) {
        this.laserPlatforms = platforms;
    }

    public Rectangle getVisionBox() {
        return visionBox;
    }

    @Override
    protected void onKilled() {
        EventBus.emit(EventBus.Event.ENEMY_CRYSTAL_LASER_STOP, this);
    }

    @Override
    public void dispose() {
        super.dispose();
        if (laserAtlas != null) laserAtlas.dispose();
        if (chargedOrbTexture != null) chargedOrbTexture.dispose();
        if (shootPoseTexture != null) shootPoseTexture.dispose();
    }
}
