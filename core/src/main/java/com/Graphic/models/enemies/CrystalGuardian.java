package com.Graphic.models.enemies;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
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
    private static final float LASER_GUN_OFFSET_X  = 80f;
    private static final float LASER_GUN_OFFSET_Y  = 60f;
    private static final float LASER_HEIGHT        = 24f;
    private static final float LASER_DAMAGE_RATE   = 0.08f;
    private static final float LASER_HOLD_DURATION = 3.0f; // Boss holds attack pose for 3 seconds

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
    private Animation<TextureAtlas.AtlasRegion> chargeLoopAnim;
    private Animation<TextureAtlas.AtlasRegion> beamAnim;
    private TextureAtlas.AtlasRegion            laserGlow;

    private float   laserStateTime    = 0f;
    private boolean windupDone        = false;
    private float   laserEndX         = 0f;
    private float   laserDamageTimer  = 0f;
    private float   beamStateTime     = 0f;
    private float   laserHoldTimer    = 0f;

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

        // ── Charge orb frames ────────────
        Array<TextureAtlas.AtlasRegion> startFrames = new Array<>();
        Array<TextureAtlas.AtlasRegion> loopFrames  = new Array<>();

        for (int i = 0; i <= 10; i++) {
            String name = String.format("crystalLaser%03d", i);
            TextureAtlas.AtlasRegion r = laserAtlas.findRegion(name);
            if (r != null) startFrames.add(r);
        }
        for (int i = 11; i <= 14; i++) {
            String name = String.format("crystalLaser%03d", i);
            TextureAtlas.AtlasRegion r = laserAtlas.findRegion(name);
            if (r != null) loopFrames.add(r);
        }

        chargeStartAnim = startFrames.size > 0 ? new Animation<>(0.05f, startFrames, Animation.PlayMode.NORMAL) : null;
        chargeLoopAnim = loopFrames.size > 0 ? new Animation<>(0.05f, loopFrames, Animation.PlayMode.LOOP) : null;

        // ── Beam animation ──────
        Array<TextureAtlas.AtlasRegion> beamFrames = new Array<>();
        for (int i = 0; i <= 11; i++) {
            String name = String.format("Laser Beam Cln_beam_shot_effect%04d", i);
            TextureAtlas.AtlasRegion r = laserAtlas.findRegion(name);
            if (r != null) beamFrames.add(r);
        }

        beamAnim = beamFrames.size > 0 ? new Animation<>(0.05f, beamFrames, Animation.PlayMode.LOOP) : null;
        laserGlow = laserAtlas.findRegion("laser_glow");
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

                        laserHoldTimer   = LASER_HOLD_DURATION;
                        changeState(EnemyState.SHOOT);
                    }
                }
                break;

            case SHOOT:
                velocity.x = 0;
                fireLaser(delta, target);

                // Wait for the boss's drawing animation to reach its final pose
                if (anim.isFinished(EnemyState.SHOOT)) {

                    // Hold the pose and keep firing the beam for exactly 3 seconds
                    laserHoldTimer -= delta;

                    if (laserHoldTimer <= 0f) {
                        enrageTimer = 0f;
                        boolean playerOnRight = playerIsOnRight(target);
                        if (playerOnRight != facingRight) {
                            beginTurn(EnemyState.WALK);
                        } else {
                            changeState(EnemyState.WALK);
                        }
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
        if (!windupDone) {
            if (chargeStartAnim != null && chargeStartAnim.isAnimationFinished(laserStateTime)) {
                windupDone = true;
            }
            return;
        }

        // ── Beam active — tick time & raycast ────────────────────────────────
        beamStateTime += delta;

        if (laserPlatforms != null) {
            laserEndX = calculateLaserHit();
        }

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

    private float calculateLaserHit() {
        float gunX  = getGunBarrelX();
        float gunY  = getGunBarrelY();
        float limit = facingRight ? (bounds.x + 2000f) : (bounds.x - 2000f);

        for (Rectangle wall : laserPlatforms) {
            if (facingRight  && wall.x < gunX) continue;
            if (!facingRight && wall.x + wall.width > gunX) continue;

            float beamTop = gunY + LASER_HEIGHT / 2f;
            float beamBot = gunY - LASER_HEIGHT / 2f;
            if (wall.y > beamTop || wall.y + wall.height < beamBot) continue;

            if (facingRight) {
                if (wall.x < limit) limit = wall.x;
            } else {
                if (wall.x + wall.width > limit) limit = wall.x + wall.width;
            }
        }
        return limit;
    }

    public boolean isPlayerInBeam(Rectangle player) {
        if (!windupDone) return false;
        float gunX  = getGunBarrelX();
        float gunY  = getGunBarrelY();
        float beamTop = gunY + LASER_HEIGHT / 2f;
        float beamBot = gunY - LASER_HEIGHT / 2f;

        boolean inYBand   = player.y < beamTop && player.y + player.height > beamBot;
        boolean inXRange  = facingRight
            ? (player.x < laserEndX && player.x + player.width > gunX)
            : (player.x + player.width > laserEndX && player.x < gunX);

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
        }
    }

    @Override
    public void render(SpriteBatch batch) {
        super.render(batch); // draws the boss sprite normally

        // The moment state leaves SHOOT, all laser visuals vanish instantly
        if (state != EnemyState.SHOOT || isDying()) return;

        float gunX = getGunBarrelX();
        float gunY = getGunBarrelY();

        // ── Charge orb at barrel ─────────────────────────────────────────────
        TextureAtlas.AtlasRegion chargeFrame = null;
        if (windupDone && chargeLoopAnim != null) {
            chargeFrame = chargeLoopAnim.getKeyFrame(laserStateTime);
        } else if (!windupDone && chargeStartAnim != null) {
            chargeFrame = chargeStartAnim.getKeyFrame(laserStateTime);
        }

        if (chargeFrame != null) {
            float orbSize = 48f;
            batch.draw(chargeFrame, gunX - orbSize / 2f, gunY - orbSize / 2f, orbSize, orbSize);
        }

        // ── Beam — only after windup ──────────────────────────────────────────
        if (windupDone && beamAnim != null) {
            TextureAtlas.AtlasRegion currentBeamFrame = beamAnim.getKeyFrame(beamStateTime);
            float beamLength = Math.abs(laserEndX - gunX);
            float drawX      = facingRight ? gunX : laserEndX;

            batch.draw(currentBeamFrame, drawX, gunY - LASER_HEIGHT / 2f, beamLength, LASER_HEIGHT);

            if (laserGlow != null) {
                float glowSize = 60f;
                batch.draw(laserGlow, gunX - glowSize / 2f, gunY - glowSize / 2f, glowSize, glowSize);
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
    public void dispose() {
        super.dispose();
        if (laserAtlas != null) laserAtlas.dispose();
    }
}
